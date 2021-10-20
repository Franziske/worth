package server;

import java.util.ArrayList;
import java.util.List;

public class Card {

	private String taskName;
	private String description;
	private CardState currentState;
	private List<CardState> history;

	public Card(String n, String d) {
		this.taskName = n;
		this.description = d;
		this.history = new ArrayList<>();
		history.add(CardState.TODO);
		this.currentState = CardState.TODO;

	}

	public Card(String n) {
		this.taskName = n;
		this.description = "";
		this.history = new ArrayList<>();
		history.add(CardState.TODO);
		this.currentState = CardState.TODO;
	}

	// jackson
	public Card() {
		this.taskName = null;
		this.description = null;
		this.history = new ArrayList<>();

		this.currentState = null;
	}

	public String getTaskName() {
		return this.taskName;
	}

	public String getDescription() {
		return this.description;
	}

	public CardState getCurrentState() {
		return this.currentState;
	}

	public List<CardState> getHistory() {
		return this.history;
	}

	public void setName(String name) {
		this.taskName = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean changeState(CardState newState) {

		// se hai tempo usa switch

		if (CardState.TODO == this.currentState) {
			if (newState == CardState.INPROGRESS) {
				this.history.add(newState);
				currentState = newState;
				return true;
			}
			return false;

		}

		if (CardState.INPROGRESS == this.currentState) {
			if (newState == CardState.TOBEREVISED || newState == CardState.DONE) {
				this.history.add(newState);
				currentState = newState;
				return true;
			}
			return false;

		}

		if (CardState.TOBEREVISED == this.currentState) {
			if (newState == CardState.INPROGRESS || newState == CardState.DONE) {
				this.history.add(newState);
				currentState = newState;
				return true;
			}
		}

		return false;

	}

}
