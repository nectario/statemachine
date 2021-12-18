package com.nektron.statemachine.impl;

import com.nektron.statemachine.KeyWrapper;
import com.nektron.statemachine.StateMachine;
import com.nektron.statemachine.StateMachineModel;
import com.nektron.statemachine.StateTransitions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Arrays;

/**
 * 
 * This is the super class for every state machine. This class should never be modified, unless for performance reasons.
 * 
 * @author Nektarios Kalogridis
 *
 * @param <INPUT>
 * @param <STATE>
 */
public class StateMachineImpl<INPUT, STATE extends Enum<STATE>,OUTPUT> implements StateMachine<INPUT, STATE, OUTPUT> {
	
	private static Logger log = LoggerFactory.getLogger(StateMachineImpl.class);
	
	protected STATE previousState;
	protected STATE currentState;
	protected INPUT currentEvent;
	protected OUTPUT value;
	
	protected List<Consumer<INPUT>> consumerOperationList = new ArrayList<>();
	protected List<NoArgConsumer> noArgConsumerOperationList = new ArrayList<>();
	protected List<BiFunction<INPUT,OUTPUT,OUTPUT>> functionOperationList = new ArrayList<>();
	
	protected String id;
	
	protected Map<KeyWrapper<INPUT>, DefaultActionTransitionWrapper<TriFunction<? extends INPUT,OUTPUT,STATE,OUTPUT>, STATE>> defaultActions = new HashMap<>() ;
	
	protected Map<KeyWrapper<INPUT>, DefaultActionTransitionWrapper<Function<OUTPUT,OUTPUT>, STATE>> defaultFunctionActions = new HashMap<>() ;
	protected Map<KeyWrapper<INPUT>, DefaultActionTransitionWrapper<Consumer<? extends INPUT>, STATE>> defaultConsumers = new HashMap<>() ;
	protected Map<KeyWrapper<INPUT>, DefaultActionTransitionWrapper<NoArgConsumer, STATE>> defaultNoArgConsumers = new HashMap<>() ;
	
	protected Map<KeyWrapper<INPUT>, TriConsumer<? extends INPUT, STATE, ? extends Throwable>> errorActions = new HashMap<>(); 

	protected StateMachineModel<INPUT, STATE, OUTPUT> stateMachineModel;
	
	protected Map<String, StateMachineModel<INPUT,STATE,OUTPUT>> stateMachineModels = new HashMap<>() ;
	
	protected boolean shouldLog = false;

	public StateMachineImpl(OUTPUT initialValue, STATE initialState) {
		
		this.value = initialValue;
		this.stateMachineModel = new StateMachineModel<INPUT,STATE,OUTPUT>(initialState);	
		this.currentState = stateMachineModel.getinitialState();
	}

	public StateMachineImpl(STATE initialState) {
		
		this.stateMachineModel = new StateMachineModel<INPUT,STATE,OUTPUT> (initialState);	
		this.currentState = stateMachineModel.getinitialState();
	}

	public StateMachineImpl(String stateMachineId, STATE initialState) {
		this.id = stateMachineId;
		this.stateMachineModel = new StateMachineModel<INPUT,STATE,OUTPUT> (stateMachineId, initialState);
		
		this.currentState = stateMachineModel.getinitialState() ;
		stateMachineModels.put(stateMachineId, stateMachineModel);
	}
	
	public void onEntry () throws Exception {
	}
	
	public void onExit () {
	}
	
	@Override
	public OUTPUT onInput(INPUT event) {
		
		try {

			currentEvent = event;
			
			StringBuilder stateTransitionString = shouldLog ? new StringBuilder() : null;
			
			StateTransitions<INPUT,STATE,OUTPUT> stateTransitions = stateMachineModel.getStateTransitions(currentState);

			if (shouldLog)
				stateTransitionString.append("Event: " + event + " -> Current State: " + currentState.toString() + " -> "); 

			
			BiFunction<INPUT,OUTPUT,OUTPUT> functionRef = stateTransitions != null ? (BiFunction<INPUT,OUTPUT,OUTPUT>) stateTransitions.getActions(event) : null;
			Consumer<INPUT> consumerFunctionRef = stateTransitions != null ? (Consumer<INPUT>) stateTransitions.getConsumerActions(event) : null;
			
			DefaultActionTransitionWrapper<TriFunction<? extends INPUT,OUTPUT,STATE,OUTPUT>, STATE> defaultActionTransitionWrapper = defaultActions.get(getKey(event));
			
			if (defaultActionTransitionWrapper == null)
				defaultActionTransitionWrapper = defaultActions.get(getKey(event.getClass()));
			
			TriFunction<INPUT,OUTPUT,STATE,OUTPUT> defaultFunctionRef = null;
			
			if (defaultActionTransitionWrapper != null) {
				defaultFunctionRef = (TriFunction<INPUT,OUTPUT,STATE,OUTPUT>) defaultActionTransitionWrapper.getAction();
			}
			
			DefaultActionTransitionWrapper<Function<OUTPUT,OUTPUT>, STATE> defaultFunctionActionTransitionWrapper = defaultFunctionActions.get(getKey(event));
			
			if (defaultFunctionActionTransitionWrapper == null)
				defaultFunctionActionTransitionWrapper = defaultFunctionActions.get(getKey(event.getClass()));
			
			Function<OUTPUT,OUTPUT> defaultFunctionActionRef = null;
			
			if (defaultFunctionActionTransitionWrapper != null) {
				defaultFunctionActionRef = (Function<OUTPUT,OUTPUT>) defaultActionTransitionWrapper.getAction();
			}
			
			DefaultActionTransitionWrapper<Consumer<? extends INPUT>, STATE> defaultConsumerTransitionWrapper = defaultConsumers.get(getKey(event));
			
			if (defaultConsumerTransitionWrapper == null)
				defaultConsumerTransitionWrapper = defaultConsumers.get(getKey(event.getClass()));
			
			Consumer<INPUT> defaultConsumerFunctionRef = null;
			
			if (defaultConsumerTransitionWrapper != null) {
				defaultConsumerFunctionRef = (Consumer<INPUT>) defaultConsumerTransitionWrapper.getAction();
			}
			
			DefaultActionTransitionWrapper<NoArgConsumer, STATE> defaultNoArgConsumerTransitionWrapper = defaultNoArgConsumers.get(getKey(event));
			
			if (defaultNoArgConsumerTransitionWrapper == null)
				defaultNoArgConsumerTransitionWrapper = defaultNoArgConsumers.get(getKey(event.getClass()));
			
			NoArgConsumer defaultNoArgConsumerFunctionRef = null;
			
			if (defaultNoArgConsumerTransitionWrapper != null) {
				defaultNoArgConsumerFunctionRef = (NoArgConsumer) defaultNoArgConsumerTransitionWrapper.getAction();
			}
			
			STATE tempCurrentState = stateTransitions != null ? stateTransitions.transition(event) : null;
			
			/**
			 *  If we have no transitions for a given state (when state machine is not in a done state), we check
			 *  to see if we have a default action to call for a given event.
			 */

			if ((tempCurrentState == null && !isDone())) {
				
				if (defaultFunctionRef != null) {
				
					if (shouldLog)
						log.info("Calling default action for state: " + currentState + " and event: " + event );
				
					
					value = defaultFunctionRef.apply(event, value, currentState);
					
					if (defaultActionTransitionWrapper.getState () != null) {
						this.previousState= currentState;
						this. currentState = defaultActionTransitionWrapper.getState();
					}
					
				} else {
					handleUnmappedTransition(event);
				}
				
				if (defaultFunctionActionRef != null) {
					if (shouldLog)
						log.info("Calling default action for state: " + currentState + " and event: " + event );

					value = defaultFunctionActionRef.apply(value);

					if (defaultActionTransitionWrapper.getState () != null) {
						this.previousState= currentState;
						this. currentState = defaultActionTransitionWrapper.getState();
					}
				} else {
					handleUnmappedTransition(event);
				}
				
				if (defaultConsumerFunctionRef != null) {
					defaultConsumerFunctionRef.accept(event);
					if (defaultConsumerTransitionWrapper.getState () != null) {
						this.previousState= currentState;
						this. currentState = defaultConsumerTransitionWrapper.getState();
					}
					
				} else {
					handleUnmappedTransition(event);
				}
				
				if (defaultNoArgConsumerFunctionRef != null) {
					
					
					defaultNoArgConsumerFunctionRef.appy();
					
					
					if (defaultNoArgConsumerTransitionWrapper.getState () != null) {
						this.previousState= currentState;
						this. currentState = defaultNoArgConsumerTransitionWrapper.getState();
					}
				} else {
					handleUnmappedTransition(event);
				}
			}
				
			if ( tempCurrentState != null) {
				
				/**
				* The below method will get called on every state exit. Good
				* for putting code that needs to execute on every transition
				* i.e. persistence.
				*/
				onExit();
				
				previousState = currentState;
				currentState = tempCurrentState;
				
				stateTransitions = stateMachineModel.getStateTransitions(currentState);
				
				/**
				* The below method will get called on every state entry. Good
				* for putting code that needs to execute on every transition
				* i.e. persistence.
				*/
				onEntry();
				
				if (functionRef != null) {
					
					value = functionRef.apply(event, value);
					if (shouldLog)
						stateTransitionString.append ( "Transitioned State, " + currentState. toString());
					
				}
				
				if (consumerFunctionRef != null) {
					
					consumerFunctionRef.accept(event);
					if (shouldLog)
					stateTransitionString.append("Transitioned State, " + currentState. toString());
					
				}
				
				if (shouldLog)
					log.info(stateTransitionString.toString());
			}
		
			if (isDone() && hasNextPhase()) {
				stateMachineModel = stateMachineModels.get(stateMachineModel.getNextPhaseStateMachineId());
				this.id = stateMachineModel.getId();
			}
			
			value =  performOperations(event,value);
			
		} catch (Exception exp ) {
			
			TriConsumer<INPUT, STATE, Exception> errorFunctionRef = (TriConsumer<INPUT, STATE, Exception>) errorActions.get(event instanceof String ? event : event.getClass ().getSimpleName());
			
			if ( errorFunctionRef != null) {
				errorFunctionRef.accept(event, currentState, exp);
			} else {
				this. handleErrorCondition(event);
			}
			
			if (shouldLog)
				log.error("State Machine Exception  " , exp);
		}
		return value;
	}
	
	public <T extends INPUT> void addTransition(STATE state, Class<T> eventType, STATE transitionedState, BiFunction<T,OUTPUT,OUTPUT> actions) {
		stateMachineModel.addTransition(state, eventType, transitionedState, actions);
	}
	
	public <T extends INPUT> void addTransition(STATE state, Class<T> eventType, STATE transitionedState) {
		stateMachineModel.addTransition(state, eventType, transitionedState);
	}
	
	public <T extends INPUT> void addTransition(STATE state, T event, STATE transitionedState, BiFunction<T,OUTPUT,OUTPUT> actions) {
		stateMachineModel.addTransition(state, event, transitionedState, actions);
	}
	
	public <T extends INPUT> void addTransition(STATE state, Class<T> eventType, STATE transitionedState, Consumer<T> actions) {
		stateMachineModel.addTransition(state, eventType, transitionedState, actions);
	}
	
	public <T extends INPUT> void addTransition(STATE state, T event, STATE transitionedState, Consumer<T> actions) {
		stateMachineModel.addTransition(state, event, transitionedState, actions);
	}
	
	public <T extends INPUT> void addTransition(STATE state, Class<T> eventType, STATE transitionedState, NoArgConsumer actions) {
		stateMachineModel.addTransition(state, eventType, transitionedState, actions);
	}
	
	public <T extends INPUT> void addTransition(STATE state, T event, STATE transitionedState, NoArgConsumer actions) {
		stateMachineModel.addTransition(state, event, transitionedState, actions);
	}
	
	public <T extends INPUT> void addTransition(STATE state, T event, STATE transitionedState) {
		stateMachineModel.addTransition(state, event, transitionedState);
	}
	
	public <T extends INPUT> void addTransition(String nextPhaseStateMachineId, STATE state, Class<T> eventType, STATE transitionedState, BiFunction<T,OUTPUT,OUTPUT> action) {
		
		StateMachineModel<INPUT,STATE, OUTPUT> aStateMachineModel = stateMachineModels.get(nextPhaseStateMachineId);
		
		if (aStateMachineModel == null) {
			aStateMachineModel = new StateMachineModel<INPUT,STATE,OUTPUT>(nextPhaseStateMachineId, state);
			stateMachineModels.put(nextPhaseStateMachineId, aStateMachineModel);
			aStateMachineModel.addTransition(state, eventType, transitionedState, action);
		}
	}
	
	public <T extends INPUT> void addTransition(String nextPhaseStateMachineId, STATE state, T event, STATE transitionedState, BiFunction<T,OUTPUT,OUTPUT> action) {
		
		StateMachineModel<INPUT,STATE,OUTPUT> aStateMachineModel = stateMachineModels.get(nextPhaseStateMachineId);
		
		if (aStateMachineModel == null) {
			aStateMachineModel = new StateMachineModel<INPUT,STATE,OUTPUT>(nextPhaseStateMachineId, state);
			stateMachineModels.put(nextPhaseStateMachineId, aStateMachineModel);
			aStateMachineModel.addTransition(state, event, transitionedState, action);
		}
	}
	
	public <T extends INPUT> void addDefaultActions(Class<T> eventType,  Consumer<T> action) {
		DefaultActionTransitionWrapper<Consumer<? extends INPUT>, STATE> defaultActionTransitionWrapper = new DefaultActionTransitionWrapper<>(action, null);
		defaultConsumers.put(new KeyWrapper<INPUT>(eventType), defaultActionTransitionWrapper);
	}
	
	public <T extends INPUT> void addDefaultActions(Class<T> eventType,  NoArgConsumer action) {
		DefaultActionTransitionWrapper<NoArgConsumer, STATE> defaultActionTransitionWrapper = new DefaultActionTransitionWrapper<>(action, null);
		defaultNoArgConsumers.put(new KeyWrapper<INPUT>(eventType), defaultActionTransitionWrapper);
	}
	
	public <T extends INPUT> void addDefaultActions(Class<T> eventType,  STATE transitionedState, NoArgConsumer action) {
		DefaultActionTransitionWrapper<NoArgConsumer, STATE> defaultActionTransitionWrapper = new DefaultActionTransitionWrapper<>(action, transitionedState);
		defaultNoArgConsumers.put(new KeyWrapper<INPUT>(eventType), defaultActionTransitionWrapper);
	}
	
    public <T extends INPUT> void addDefaultActions(Class<T> eventType, TriFunction <T,OUTPUT,STATE,OUTPUT> action) {
		DefaultActionTransitionWrapper<TriFunction<? extends INPUT,OUTPUT,STATE,OUTPUT>, STATE> defaultActionTransitionWrapper = new DefaultActionTransitionWrapper<>(action, null);
		defaultActions.put(new KeyWrapper<INPUT>(eventType), defaultActionTransitionWrapper);
	}
	
	public <T extends INPUT> void addDefaultActions(Class<T> eventType, STATE transitionedState, TriFunction<T,OUTPUT,STATE,OUTPUT> action) {
		DefaultActionTransitionWrapper<TriFunction<? extends INPUT,OUTPUT,STATE,OUTPUT>, STATE> defaultActionTransitionWrapper = new DefaultActionTransitionWrapper<>(action, transitionedState);
		defaultActions.put(new KeyWrapper<INPUT>(eventType), defaultActionTransitionWrapper);
	}
	
	 public <T extends INPUT> void addDefaultActions(Class<T> eventType, Function<OUTPUT,OUTPUT> action) {
		DefaultActionTransitionWrapper<Function<OUTPUT,OUTPUT>, STATE> defaultActionTransitionWrapper = new DefaultActionTransitionWrapper<Function<OUTPUT, OUTPUT>, STATE>(action, null);
		defaultFunctionActions.put(new KeyWrapper<INPUT>(eventType), defaultActionTransitionWrapper);
	}
		
	public <T extends INPUT> void addDefaultActions(Class<T> eventType, STATE transitionedState, Function<OUTPUT,OUTPUT> action) {
		DefaultActionTransitionWrapper<Function<OUTPUT,OUTPUT>, STATE> defaultActionTransitionWrapper = new DefaultActionTransitionWrapper<>(action, transitionedState);
		defaultFunctionActions.put(new KeyWrapper<INPUT>(eventType), defaultActionTransitionWrapper);
	}
	
	public <T extends INPUT> void addDefaultActions(T event , TriFunction <T,OUTPUT,STATE,OUTPUT> action) {
		DefaultActionTransitionWrapper<TriFunction<? extends INPUT,OUTPUT,STATE,OUTPUT>, STATE> defaultActionTransitionWrapper = new DefaultActionTransitionWrapper<TriFunction<? extends INPUT, OUTPUT, STATE, OUTPUT>, STATE>(action, null);
		defaultActions.put( new KeyWrapper<INPUT>(event), defaultActionTransitionWrapper);
		new DefaultActionTransitionWrapper<>(action, null);
	}
	
	public <T extends INPUT> void addDefaultActions(T event, STATE transitionedState, TriFunction<T,OUTPUT,STATE,OUTPUT> action ) {
		DefaultActionTransitionWrapper<TriFunction<? extends INPUT,OUTPUT,STATE,OUTPUT>, STATE> defaultActionTransitionWrapper = new DefaultActionTransitionWrapper<>(action, transitionedState);
		defaultActions.put(new KeyWrapper<INPUT>(event), defaultActionTransitionWrapper);
	}
	
	public <T extends INPUT> void addErrorAction(Class<T> eventType, TriConsumer<T ,STATE, ? extends Throwable > action) {
		errorActions.put(new KeyWrapper<INPUT>(eventType), action);
	}

	public <T extends INPUT> void addErrorAction(T event, TriConsumer<T ,STATE, ? extends Throwable > action) {
		errorActions.put(new KeyWrapper<INPUT>(event) , action);
	}
	
	/**
	 * 
	 * Links two state machines (for two phase state machines). What this means: when the first goes to a done state, the next sone is activated.
	 * 
	 * @param stateMachineId
	 * @param nextPhaseStateMachineId
	 */
	public void linkStateMachines(String stateMachineId, String nextPhaseStateMachineId) {
		
		StateMachineModel<INPUT,STATE, OUTPUT> aStateMachineModel = stateMachineModels.get(stateMachineId);
		
		if (aStateMachineModel != null)
			aStateMachineModel.setNextPhaseStateMachineId(nextPhaseStateMachineId );
		else
			throw new IllegalArgumentException("Unable to link state 11achines ! State Machine with id "+stateMachineId+" does not exist.");
		
	}
	
	private OUTPUT performOperations(INPUT event, OUTPUT value) {
		
		if (functionOperationList != null && functionOperationList.size() > 0) {
			for (BiFunction<INPUT,OUTPUT,OUTPUT> operation : functionOperationList) {
				this.value = operation.apply(event, value);
			}
		}
		
		if (consumerOperationList != null && consumerOperationList.size() > 0) {
			for (Consumer<INPUT> operation : consumerOperationList) {
				operation.accept(event);
			}
		}
		
		if (noArgConsumerOperationList != null && noArgConsumerOperationList.size() > 0) {
			for (NoArgConsumer operation : noArgConsumerOperationList) {
				operation.appy();
			}
		}
		return this.value;
	}
	
	/**
	 * The id of the state machine.
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * 
	 * The state machine id for the next phase.
	 * 
	 * @return
	 */
	public String getNextPhaseStateMachineId() {
		return stateMachineModel.getNextPhaseStateMachineId();
	}
	
	/**
	 * The current state
	 */
	public STATE getCurrentState() {
		return currentState;
	}
	
	public OUTPUT getValue() {
		return value;
	}
	/**
	 * The previous state
	 */
	public STATE getPreviousState() {
		return previousState;
	}
	
	/**
	 * The current event
	 * @return
	 */
	public INPUT getCurrentEvent() {
		return currentEvent;
	}
	
	public StateMachineModel<INPUT,STATE, OUTPUT> getStateMachineModel() {
		return stateMachineModel;
	}
	
	public boolean isDone() {
		return stateMachineModel.isDoneState(currentState);
	}
	
	public boolean isTerminated() {
		return stateMachineModel.isTerminalState(currentState);
	}
	
	public void setStateMachineModel(StateMachineModel<INPUT,STATE, OUTPUT> stateMachineModel) {
		this.stateMachineModel = stateMachineModel;
		this.id = stateMachineModel.getId();
		currentState = stateMachineModel.getinitialState();
		previousState = null;
	}
	
	public boolean hasNextPhase() {
		return stateMachineModel.hasNextPhase();
	}
	
	public void addDoneState (STATE doneState ) {
		stateMachineModel.addDoneState( doneState);
	}
		
	public void addTenainalState(STATE terminalState) {
		stateMachineModel.addTerminalState(terminalState);
	}
	
	public void addDoneState(String stateMachineId, STATE doneState) {
		
		StateMachineModel <INPUT, STATE, OUTPUT> aStateMachineModel = stateMachineModels.get(stateMachineId) ;
		
		if (aStateMachineModel != null)
			aStateMachineModel.addDoneState ( doneState);
		else
			throw new IllegalArgumentException( "Can't add done states to state machine with id " +stateMachineId+ ". State machine does not exist!");
	}
	
	
	public void addTerminalState(String stateMachineId, STATE terminalState) {
		StateMachineModel<INPUT, STATE, OUTPUT> aStateMachineModel = stateMachineModels.get( stateMachineId);
		
		if (aStateMachineModel != null) {
			aStateMachineModel.addTerminalState(terminalState);
		} else {
			throw new IllegalArgumentException("Can't add terminal states to state machine with id "+stateMachineId+". State machine does not exist!");
		}
	}
	
	public void handleErrorCondition(INPUT event) {
	
	}
	
	public void handleUnmappedTransition(INPUT event) {
		//log.warn("No state transitions defined for state: "+currentState + " and event: " + event);
	}
	
	public <T extends INPUT> void addOperations(Consumer<T> action) {
		this.consumerOperationList.add((Consumer<INPUT>) action);
	}
	
	public  void addOperations(NoArgConsumer action) {
		this.noArgConsumerOperationList.addAll(Arrays.asList(action));
	}

	public <T extends INPUT> void addOperations(BiFunction<T,OUTPUT,OUTPUT> action) {
		this.functionOperationList.add((BiFunction<INPUT, OUTPUT, OUTPUT>) action);
	}
	
	private static class DefaultActionTransitionWrapper<U, V> {	
		private final U action;
		private final V state;
		
		DefaultActionTransitionWrapper(U action, V state) {
			this.action = action;
			this.state = state;
		}
		
		public U getAction() {
			return action; 
		}
		
		public V getState() {
			return state;
		}
	}
	
	public String toString() {
		return "State Machine: "+id;
	}
	
	private KeyWrapper<INPUT> getKey(INPUT event) {
		return new KeyWrapper<INPUT>(event);
	}
	
	private KeyWrapper<INPUT> getKey(Class<?> eventClass) {
		return new KeyWrapper<INPUT>(eventClass);
	}	
	
	
}