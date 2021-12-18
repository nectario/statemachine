package statemachine;


import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.nektron.statemachine.StateMachine;
import com.nektron.statemachine.impl.StateMachineImpl;
import com.nektron.statemachine.state.State;

import java.util.function.BiConsumer;

public class StateMachineTest {

	private final StateMachine<String,State, String> stateMachine = new StateMachineImpl<>("SM-TEST", State.START);
	private final StateMachine<SMEvent,State, Double> stateMachineTyped = new StateMachineImpl<>("SM-TEST2", State.START);
	private final StateMachine<SMEvent,State, Double> stateMachineDefaultActions = new StateMachineImpl<>("SM-TEST3", State.START);

	private String strVal = "test";
	
	@Before
	public void setUp() throws Exception {
			
	}

	@Test
	public void test() {
		
		stateMachine.addTransition(State.START, "NewEvt", State.NEW);
		
		stateMachine.onInput("NewEvt");

		assertSame(stateMachine.getCurrentState(), State.NEW);
		
		stateMachineTyped.addTransition(State.START, SMEvent.class, State.NEW);
		stateMachineTyped.onInput(new SMEvent("Test"));
		
		stateMachineTyped.addTransition(State.NEW, CustomSMEvent.class, State.COMPUTE);
		stateMachineTyped.onInput(new CustomSMEvent("Test"));

		assertSame(stateMachineTyped.getCurrentState(), State.COMPUTE);
		
		stateMachineTyped.addTransition(State.COMPUTE, CustomSMEvent.class, State.COMPUTE, this::getValue);
		
		stateMachineTyped.addOperations(this::transform);
		stateMachineTyped.addOperations(this::nullify);
		
		Double value = stateMachineTyped.onInput(new CustomSMEvent("Test"));

		assertEquals(3.2 + 1, value, 0.0);
		assertNull(strVal);
	
		stateMachineTyped.addTransition(State.COMPUTE, new CustomSMEvent("custom"), State.DONE, this::getValue);
		value = stateMachineTyped.onInput(new CustomSMEvent("custom"));
		
		assertEquals(State.DONE,stateMachineTyped.getCurrentState());
		assertEquals(4.2, value, 0.0);
		
		stateMachineTyped.addDefaultActions(DefaultAction.class, this::handleDefaultAction);
		value = stateMachineTyped.onInput(new DefaultAction("custom default action"));

		assertEquals(1, (double) value, 0.0);
		assertNull(strVal);
	}

	@Test
	public void testDefaultActions() {
		
		stateMachineDefaultActions.addTransition(State.START, SMEvent.class, State.NEW, this::increment);
		
		stateMachineDefaultActions.addDefaultActions(new DefaultAction("custom default action 2"), this::incrementDefault);
		Double value = stateMachineDefaultActions.onInput(new DefaultAction("custom default action 2"));
		assertEquals(1.0, value, 0.0);
		
		value = stateMachineDefaultActions.onInput(new DefaultAction("custom default action 2"));
		assertEquals(2.0, value, 0.0);
		
		value = stateMachineDefaultActions.onInput(new DefaultAction("custom default action 3"));
		assertEquals(2.0, value, 0.0);
		
		value = stateMachineDefaultActions.onInput(new SMEvent("Test"));
		assertEquals(3.0, value, 0.0);
	}
	
	public Double getMinDoubleValue(CustomSMEvent event, Double value) {
		return Double.MIN_VALUE;
	}

	public Double getValue(CustomSMEvent event, Double value) {
		return 3.2;
	}
	
	public Double handleDefaultAction(DefaultAction event, Double value, State state) {
		strVal = "defaultActionCalled";
		return Double.MIN_NORMAL;
	}
	
	public Double incrementDefault(DefaultAction event, Double value, State state) {
		if (value == null) {
			value = 0.0;
		}
		return value + 1.0;
	}
	
	public Double increment(SMEvent event, Double value) {
		if (value == null) {
			value = 0.0;
		}
		return value + 1.0;
	}
	
	public Double transform(CustomSMEvent event, Double value) {
		value = value + 1;
		return value;
	}
	
	public void nullify(CustomSMEvent event) {
		strVal = null;
	}
	
	public class SMEvent {
		
		protected String name;
		
		public SMEvent(String value) {
			this.name = value;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SMEvent other = (SMEvent) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}

		private StateMachineTest getOuterType() {
			return StateMachineTest.this;
		}
		
	}
	
	public class CustomSMEvent extends SMEvent {
		
		public CustomSMEvent(String value) {
		
			super(value);
			
			this.name = value;
		}
	}
	
	public class DefaultAction extends CustomSMEvent {
		
		protected String name;
		
		public DefaultAction(String value) {
			super(value);
			this.name = value;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DefaultAction other = (DefaultAction) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}

		private StateMachineTest getOuterType() {
			return StateMachineTest.this;
		}
		
	}
}
