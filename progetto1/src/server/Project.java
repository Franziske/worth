package server;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonIgnore;

import exceptions.DuplicateException;
import exceptions.NotFoundException;

public class Project {

	private String name;
	private User creator;
	private Set<String> members;
	private ArrayList<Card> TODO;
	private ArrayList<Card> INPROGRESS;
	private ArrayList<Card> TOBEREVISED;
	private ArrayList<Card> DONE;
	private InetAddress chatAddress;

	public Project(String n, User c, InetAddress ca) {

		// worth Controlla che il nome sia univoco
		this.name = n;
		this.creator = c;
		this.members = new TreeSet<>();
		members.add(c.getnikName());
		this.TODO = new ArrayList<>();
		this.INPROGRESS = new ArrayList<>();
		this.TOBEREVISED = new ArrayList<>();
		this.DONE = new ArrayList<>();
		this.chatAddress = ca;

	}

	// jackson
	public Project() {

		this.name = null;
		this.creator = null;
		this.members = new TreeSet<>();

		this.TODO = new ArrayList<>();
		this.INPROGRESS = new ArrayList<>();
		this.TOBEREVISED = new ArrayList<>();
		this.DONE = new ArrayList<>();
		this.chatAddress = null;
	}

	public String getName() {
		return this.name;
	}

	public User getCreator() {
		return this.creator;
	}

	public List<Card> getTodoCards() {
		return this.TODO;
	}

	public List<Card> getInprogressCards() {
		return this.INPROGRESS;
	}

	public List<Card> getToberevisedCards() {
		return this.TOBEREVISED;
	}

	public List<Card> getDoneCards() {
		return this.DONE;
	}

	public InetAddress getChatAddress() {
		return chatAddress;
	}

	@JsonIgnore
	public List<Card> getAllCards() {

		List<Card> all = new ArrayList<>(); // ?????
		all.addAll(TODO);
		all.addAll(INPROGRESS);
		all.addAll(TOBEREVISED);
		all.addAll(DONE);

		return all;
	}

	Card getCard(String n) throws NotFoundException {

		for (Card c : this.getAllCards()) {
			if (n.equals(c.getTaskName()))
				return c;
		}
		throw new NotFoundException();
	}

	public List<String> getMembers() {
		return new ArrayList<>(this.members);
	}

	public void addCard(String c) throws DuplicateException {
		try {
			this.getCard(c);
		} catch (NotFoundException e) {
			this.TODO.add(new Card(c));

			return;
		}
		throw new DuplicateException();
	}

	void addCard(String n, String descrip) throws DuplicateException {
		try {
			this.getCard(n);
		} catch (NotFoundException e) {
			this.TODO.add(new Card(n, descrip));
			for (Card c : TODO)
				System.out.println(c.getTaskName() + c.getDescription());
			return;
		}
		throw new DuplicateException("Card already exists");
	}

	public void addCards(List<Card> cards) {

		for (Card c : cards) {

			CardState cstate = c.getCurrentState();

			if (CardState.TODO == cstate && !(this.TODO.contains(c)))
				this.TODO.add(c);
			if (CardState.INPROGRESS == cstate && !(this.INPROGRESS.contains(c)))
				this.INPROGRESS.add(c);
			if (CardState.TOBEREVISED == cstate && !(this.TOBEREVISED.contains(c)))
				this.TOBEREVISED.add(c);
			if (CardState.DONE == cstate && !(this.DONE.contains(c)))
				this.DONE.add(c);

		}
	}

	void removeCard(String n) throws NotFoundException {

		for (Card c : this.DONE) {
			if (c.getTaskName().equals(n)) {
				DONE.remove(c);
				return;
			}
		}

		for (Card c : this.INPROGRESS) {
			if (c.getTaskName().equals(n)) {
				DONE.remove(c);
				return;
			}
		}

		for (Card c : this.TOBEREVISED) {
			if (c.getTaskName().equals(n)) {
				DONE.remove(c);
				return;
			}
		}
		for (Card c : this.DONE) {
			if (c.getTaskName().equals(n)) {
				DONE.remove(c);
				return;
			}
		}
		throw new NotFoundException("Card not found");
	}

	boolean changeCardState(String n, CardState to) throws NotFoundException {

		Card c = this.getCard(n);
		boolean b = c.changeState(to);
		if (b) {
			this.removeCard(n);
			if (to == CardState.TODO)
				TODO.add(c);
			if (to == CardState.INPROGRESS)
				INPROGRESS.add(c);
			if (to == CardState.TOBEREVISED)
				TOBEREVISED.add(c);
			if (to == CardState.DONE)
				DONE.add(c);
		}
		return b;
	}

	boolean addMember(User usr) {

		// if(this.members.contains(usr.getnikName())) return false;
		this.members.add(usr.getnikName());
		return true;
	}

}
