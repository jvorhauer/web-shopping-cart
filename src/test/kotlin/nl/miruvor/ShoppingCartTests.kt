import akka.actor.testkit.typed.javadsl.TestKitJunitResource
import akka.actor.typed.javadsl.AskPattern.ask
import akka.pattern.StatusReply
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger


class ShoppingCartTests {
  private val counter = AtomicInteger()
  private val testKit = TestKitJunitResource(
    """akka.persistence.journal.plugin = "akka.persistence.journal.inmem" 
       akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"  
       akka.persistence.snapshot-store.local.dir = "target/snapshot-${UUID.randomUUID()}"  
    """
  )

  private fun newCartId() = "cart-" + counter.incrementAndGet()

  @Test
  fun `should add item`() {
    val cart = testKit.spawn(ShoppingCart.create(newCartId()))
    val probe = testKit.createTestProbe<StatusReply<Summary>>()
    cart.tell(AddItem("foo", 42, probe.ref))
    val result = probe.receiveMessage()
    assertThat(result.isSuccess).isTrue
    assertThat(result.value.items["foo"]).isEqualTo(42)
  }

  @Test
  fun `ask for an additional item`() {
    val cart = testKit.spawn(ShoppingCart.create(newCartId()))
    val result = ask(cart, { replyTo -> AddItem("foo", 42, replyTo) }, Duration.ofSeconds(2), testKit.scheduler())
    val t = result.toCompletableFuture().get()
    assertThat(t.isSuccess).isTrue
    assertThat(t.value.items.containsKey("foo")).isTrue
  }

  @Test
  fun `should reject already added item`() {
    val cart = testKit.spawn(ShoppingCart.create(newCartId()))
    val probe = testKit.createTestProbe<StatusReply<Summary>>()
    cart.tell(AddItem("foo", 42, probe.ref))
    assertThat(probe.receiveMessage().isSuccess).isTrue
    cart.tell(AddItem("foo", 17, probe.ref))
    assertThat(probe.receiveMessage().isError).isTrue
  }

  @Test
  fun `should reject item with negative quantity`() {
    val cart = testKit.spawn(ShoppingCart.create(newCartId()))
    val probe = testKit.createTestProbe<StatusReply<Summary>>()
    cart.tell(AddItem("foo", -42, probe.ref))
    assertThat(probe.receiveMessage().isError).isTrue
  }

  @Test
  fun `should adjust item quantity`() {
    val cart = testKit.spawn(ShoppingCart.create(newCartId()))
    val probe = testKit.createTestProbe<StatusReply<Summary>>()
    cart.tell(AddItem("foo", 42, probe.ref))
    assertThat(probe.receiveMessage().isSuccess).isTrue
    cart.tell(AdjustItemQuantity("foo", 43, probe.ref))
    val result = probe.receiveMessage()
    assertThat(result.isSuccess).isTrue
    assertThat(result.value.items["foo"]).isEqualTo(43)
  }

  @Test
  fun `should remove item`() {
    val cart = testKit.spawn(ShoppingCart.create(newCartId()))
    val probe = testKit.createTestProbe<StatusReply<Summary>>()
    cart.tell(AddItem("foo", 42, probe.ref))
    assertThat(probe.receiveMessage().isSuccess).isTrue
    cart.tell(RemoveItem("foo", probe.ref))
    val result = probe.receiveMessage()
    assertThat(result.isSuccess).isTrue
    assertThat(result.value.isEmpty()).isTrue
  }

  @Test
  fun `should reject removing non-existent item`() {
    val cart = testKit.spawn(ShoppingCart.create(newCartId()))
    val probe = testKit.createTestProbe<StatusReply<Summary>>()
    cart.tell(AddItem("foo", 42, probe.ref))
    assertThat(probe.receiveMessage().isSuccess).isTrue
    cart.tell(RemoveItem("bar", probe.ref))
    val result = probe.receiveMessage()
    assertThat(result.isError).isTrue
  }

  @Test
  fun `adjust item with negative quantity should remove item`() {
    val cart = testKit.spawn(ShoppingCart.create(newCartId()))
    val probe = testKit.createTestProbe<StatusReply<Summary>>()
    cart.tell(AddItem("foo", 42, probe.ref))
    assertThat(probe.receiveMessage().isSuccess).isTrue
    cart.tell(AdjustItemQuantity("foo", -5, probe.ref))
    val result = probe.receiveMessage()
    assertThat(result.isSuccess).isTrue
    assertThat(result.value.isEmpty()).isTrue
  }

  @Test
  fun `should check out`() {
    val cart = testKit.spawn(ShoppingCart.create(newCartId()))
    val probe = testKit.createTestProbe<StatusReply<Summary>>()
    cart.tell(AddItem("foo", 42, probe.ref))
    assertThat(probe.receiveMessage().isSuccess).isTrue
    cart.tell(Checkout(probe.ref))
    val result = probe.receiveMessage()
    assertThat(result.isSuccess).isTrue
    assertThat(result.value.checkedOut).isTrue
  }

  @Test
  fun `should not check out twice`() {
    val cart = testKit.spawn(ShoppingCart.create(newCartId()))
    val probe = testKit.createTestProbe<StatusReply<Summary>>()
    cart.tell(AddItem("foo", 42, probe.ref))
    assertThat(probe.receiveMessage().isSuccess).isTrue
    cart.tell(Checkout(probe.ref))
    val result = probe.receiveMessage()
    assertThat(result.isSuccess).isTrue
    assertThat(result.value.checkedOut).isTrue

    cart.tell(Checkout(probe.ref))
    assertThat(probe.receiveMessage().isError).isTrue

    cart.tell(AddItem("bar", 11, probe.ref))
    assertThat(probe.receiveMessage().isError).isTrue
  }

  @Test
  fun `should recover from restart of actor system`() {
    val cartId = newCartId()
    val cart = testKit.spawn(ShoppingCart.create(cartId))
    val probe = testKit.createTestProbe<StatusReply<Summary>>()
    cart.tell(AddItem("foo", 42, probe.ref))
    assertThat(probe.receiveMessage().isSuccess).isTrue

    testKit.stop(cart)

    val restarted = testKit.spawn(ShoppingCart.create(cartId))
    val stateProbe = testKit.createTestProbe<StatusReply<Summary>>()
    restarted.tell(Get(stateProbe.ref))
    val state = stateProbe.receiveMessage()
    assertThat(state.value.items["foo"]).isEqualTo(42)
  }
}
