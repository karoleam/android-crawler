package it.unina.androidripper.planning;

import it.unina.androidripper.model.Abstractor;

import java.util.Random;


public class AlternativeUser extends SimpleUser {

	// Placeholder for branch user - throws an exception if instantiation is attempted

	public AlternativeUser(Abstractor a) {
		super (a);
		throw (new UnsupportedOperationException());
	}

	public AlternativeUser(Abstractor a, Random random) {
		super (a,random);
		throw (new UnsupportedOperationException());
	}

}
