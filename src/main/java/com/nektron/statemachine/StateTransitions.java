package com.nektron.statemachine;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import com.nektron.statemachine.StateMachine.NoArgConsumer;

public class StateTransitions<INPUT, STATE extends Enum<STATE>,OUTPUT> {

	private STATE state;
	
	private Map<KeyWrapper<INPUT>, STATE> stateTransitions = new HashMap<>();
	private Map<KeyWrapper<INPUT>, BiFunction<? extends INPUT,OUTPUT,OUTPUT>> stateTransitionActions = new HashMap<>();
	private Map<KeyWrapper<INPUT>, Consumer<? extends INPUT>> stateTransitionConsumerActions = new HashMap<>();
	private Map<KeyWrapper<INPUT>, NoArgConsumer> stateTransitionNoArgConsumerActions = new HashMap<>();
	
	public StateTransitions(STATE state) {
		this.state = state;
	}

	public BiFunction<? extends INPUT,OUTPUT,OUTPUT> getActions(INPUT event) {
		
		BiFunction<? extends INPUT,OUTPUT,OUTPUT> functionRef = null;
		
		if (stateTransitions != null && event != null) {
			
			 functionRef = stateTransitionActions.get(getKey(event));
			 
			if (functionRef != null)
				return functionRef;
			else
				functionRef = stateTransitionActions.get(getKey(event.getClass()));
		}
		
		return functionRef;
	}
	
	public Consumer<? extends INPUT> getConsumerActions(INPUT event) {
		
		Consumer<? extends INPUT> consumerFunctionRef = null;
		
		if (stateTransitions != null && event != null) {
			
			 consumerFunctionRef = stateTransitionConsumerActions.get(getKey(event));
			 
			if (consumerFunctionRef != null)
				return consumerFunctionRef;
			else
				consumerFunctionRef = stateTransitionConsumerActions.get(getKey(event.getClass()));
		}
		
		return consumerFunctionRef;
	}

	public STATE transition(INPUT event) {
		
		if (stateTransitions != null) {
			
			STATE state = stateTransitions.get(getKey(event));
			
			if (state == null) {
				state =  stateTransitions.get(getKey(event.getClass()));
			}
			
			return state;
		}
		
		return null;
	}

	private KeyWrapper<INPUT> getKey(INPUT event) {
		return new KeyWrapper<INPUT>(event);
	}
	
	private KeyWrapper<INPUT> getKey(Class<?> eventClass) {
		return new KeyWrapper<INPUT>(eventClass);
	}
	
	public boolean hasTransitions() {
		return stateTransitions != null && ! stateTransitions.isEmpty();
	}

    public <T extends INPUT> void addTransition(Class<T> event, STATE state, BiFunction<T,OUTPUT,OUTPUT> action){
    	KeyWrapper<INPUT> key = new KeyWrapper<>(event);
  
    	stateTransitions.put(key, state);
		stateTransitionActions.put(key, action);
	}

	public <T extends INPUT > void addTransition(INPUT event, STATE state, BiFunction<T,OUTPUT,OUTPUT> action) {
		KeyWrapper<INPUT> key = new KeyWrapper<>(event);
		
		stateTransitions.put(key, state);
		stateTransitionActions.put(key, action);
	}
	
	public <T extends INPUT> void addTransition(Class<T> event, STATE state, Consumer<T> action){
    	KeyWrapper<INPUT> key = new KeyWrapper<>(event);
  
    	stateTransitions.put(key, state);
		stateTransitionConsumerActions.put(key, action);
	}

	public <T extends INPUT > void addTransition(INPUT event, STATE state, Consumer<T> action) {
		KeyWrapper<INPUT> key = new KeyWrapper<>(event);
		
		stateTransitions.put(key, state);
		stateTransitionConsumerActions.put(key, action);
	}
	
	public <T extends INPUT> void addTransition(Class<T> event, STATE state, NoArgConsumer action){
    	KeyWrapper<INPUT> key = new KeyWrapper<>(event);
  
    	stateTransitions.put(key, state);
		stateTransitionNoArgConsumerActions.put(key, action);
	}

	public <T extends INPUT > void addTransition(INPUT event, STATE state, NoArgConsumer action) {
		KeyWrapper<INPUT> key = new KeyWrapper<>(event);
		
		stateTransitions.put(key, state);
		stateTransitionNoArgConsumerActions.put(key, action);
	}
	
	public <T extends INPUT> void addTransition(Class<T> event, STATE state){
    	KeyWrapper<INPUT> key = new KeyWrapper<>(event);
    	stateTransitions.put(key, state);
	}

	public <T extends INPUT > void addTransition(INPUT event, STATE state) {
		KeyWrapper<INPUT> key = new KeyWrapper<>(event);
		stateTransitions.put(key, state);
	}

	/**
	 * 
	 * The state
	 * 
	 * @return
	 */
	public STATE getState() {
		return state;
	}
	
}
