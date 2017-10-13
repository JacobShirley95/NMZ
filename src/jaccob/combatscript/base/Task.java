package jaccob.combatscript.base;

import org.powerbot.script.ClientAccessor;
import org.powerbot.script.ClientContext;

public abstract class Task<T extends ClientContext<?>> extends ClientAccessor<T> {
	public Task(T arg0) {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	public abstract boolean activate();
	
	public abstract boolean run();
}
