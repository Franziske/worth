package server;

public class User {

	private String nikName;
	private String password;
	// private ArrayList<String> UserProject;

	// projects created????

	public User(String n, String p) {

		this.nikName = n;
		this.password = p;
		// this.UserProject = new ArrayList<>();
	}

	// jackson
	public User() {
		this.nikName = null;
		this.password = null;
	}

	public String getnikName() {
		return this.nikName;
	}

	public String getPassword() {
		return this.password;
	}

	/*
	 * public void addProject (String pname) { this.UserProject.add(pname); return;
	 * }
	 */

}
