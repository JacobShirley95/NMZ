package jaccob.combatscript.base;

import org.powerbot.script.ClientContext;

public abstract class InteractingTask<T extends ClientContext<?>> extends Task<T> {

	public InteractingTask(T arg0) {
		super(arg0);
	}

	public abstract boolean track();
	
}
