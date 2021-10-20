package server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import exceptions.MissingFolderException;

public class DataBase {

	private String DBName;
	private final ObjectMapper objectMapper;
	private Path DBpath;
	

	public DataBase(String directoryName) throws IOException {

		this.objectMapper = new ObjectMapper();
		Path path = Paths.get("./" + directoryName + "/");
		this.DBpath = path;
		try {
			this.DBName = directoryName;

			if (Files.notExists(DBpath)) {
				Files.createDirectories(DBpath);
				System.out.println("Directory DB created correctly in" + DBpath.toAbsolutePath().toString());

				Path pathproj = Paths.get("./" + directoryName + "/Projects/");
				Files.createDirectories(pathproj);

				System.out.println("Project directory created correctly in DB");

				// creo una cartella per gli utenti

				Path pathusr = Paths.get("./" + directoryName + "/Users/");
				Files.createDirectories(pathusr);

				System.out.println("Users directory created correctly in DB");

			} else
				System.out.println("Directory DB already exists : " + DBpath.toAbsolutePath().toString());
			Path pathproj = Paths.get("./" + directoryName + "/Projects/");
			Files.createDirectories(pathproj);
			System.out.println("Project directory correctly inizialized in DB");

			Path pathusr = Paths.get("./" + directoryName + "/Users/");
			Files.createDirectories(pathusr);
			System.out.println("Users directory correctly inizialized in DB");

		} catch (IOException e) {

			System.err.println("Directory DB cration failed " + e.getMessage());
			return;

		}

	}

	public DataBase() throws IOException {
		this("DB");
	}

	public List<Project> getProjects() throws StreamReadException, DatabindException, IOException {
		ArrayList<Project> prgs = new ArrayList<>();
		// ArrayList<Card> crds = new ArrayList<Card>();

		// cartella progetti
		File prgsFolder = new File("./" + this.DBName + "/Projects/");

		// lista dei File nella cartella Projects

		File[] fileInPrgsFolder = prgsFolder.listFiles();

		for (File f : fileInPrgsFolder) {
			if (f.isFile()) {
				prgs.add(objectMapper.readValue(f, Project.class));

			}

		}
		return prgs;
	}

	/*
	 * for(File p : currentPrgsFolder) {
	 *
	 * File[] cardInP = p.listFiles(); for(File c : cardInP) {
	 * crds.add(objectMapper.readValue(c, Card.class)); }
	 */

	public List<User> getUsers() throws StreamReadException, DatabindException, IOException {
		ArrayList<User> users = new ArrayList<>();

		File usersFolder = new File("./" + this.DBName + "/Users/");

		File[] fileInUsersFolder = usersFolder.listFiles();

		for (File f : fileInUsersFolder) {
			if (f.isFile()) {
				users.add(objectMapper.readValue(f, User.class));

			}
		}
		return users;

	}

	public void addProject(Project p) throws IOException {
		Path pathp = Paths.get("./" + this.DBName + "/Projects/" + p.getName() + "/");

		if (Files.notExists(pathp)) {
			Files.createDirectories(pathp);
			TreeSet<Path> dirs = new TreeSet<>(Files.list(pathp).toList());

			File filep = new File("./" + this.DBName + "/Projects/" + p.getName() + "File.json");

			if (filep.createNewFile()) {
				objectMapper.writeValue(filep, p);
			}

			System.out.println(dirs);
			System.out.println(objectMapper.writeValueAsString(p) + "added in Projects");
		} else {
			TreeSet<Path> dirs = new TreeSet<>(Files.list(pathp).toList());
			System.out.println(dirs);
			System.out.println(p.getName() + "already exists in Projects");
		}
	}

	public void uploadCardState(Project p, Card newc) throws StreamWriteException, DatabindException, IOException {
		File filec = new File("./" + this.DBName + "/Projects/" + p.getName() + "/" + newc.getTaskName() + ".json");
		if (filec.delete()) {
			objectMapper.writeValue(filec, newc);

			this.refreshProject(p);

			System.out.println(newc.getTaskName() + " State uploadedin DB ");

		}

	}

	public void addCardToProject(Card c, Project to) {
		try {
			File filec = new File("./" + this.DBName + "/Projects/" + to.getName() + "/" + c.getTaskName() + ".json");
			if (filec.createNewFile()) {

				objectMapper.writeValue(filec, c);

				this.refreshProject(to);

				System.out.println(objectMapper.writeValueAsString(c) + " added to project:" + to.getName());
			}

			else
				System.out.println(c.getTaskName() + " already exists in project:" + to.getName());

		} catch (IOException e) {

			e.printStackTrace();
		}

	}

	public void addUser(User u) {
		try {
			File fileu = new File("./" + this.DBName + "/Users/" + u.getnikName() + ".json");
			File filed = new File("./" + this.DBName + "/Users/");
			// Path p = Paths.get("./" + this.DBName + "/Users/");

			System.out.println(filed.isDirectory());

			if (fileu.createNewFile()) {

				objectMapper.writeValue(fileu, u);

				System.out.println(objectMapper.writeValueAsString(u) + " added to Users:");
			}

			else
				System.out.println(u.getnikName() + " already exists in Users:");

		} catch (IOException e) {

			e.printStackTrace();
		}
	}

	/*
	 * try { Path path = Paths.get("./" + this.DBName + "/Users/" + u.getnikName() +
	 * ".json");
	 *
	 * if(Files.notExists(path)) {
	 *
	 * Files.createFile(path);
	 *
	 * objectMapper.writeValue(, u);
	 *
	 * System.out.println(objectMapper.writeValueAsString(u) +
	 * " added to WORTH Users"); }
	 *
	 * else System.out.println(u.getnikName() + " already exists WORTH Users");
	 *
	 * } catch (IOException e){
	 *
	 * e.printStackTrace(); }
	 */

	// static??
	public void deleteProject(Project p) throws IOException {

		File projectFolder = new File("./" + this.DBName + "/Projects/" + p.getName());

		File[] files = projectFolder.listFiles();

		if (files != null) {
			for (File f : files)
				f.delete();
		}

		projectFolder.delete();

		File pFile = new File("./" + this.DBName + "/Projects/" + p.getName() + "File.json");
		pFile.delete();

		// aggiungi return bool per verifica

	}

	public void refreshProject(Project p) {

		File filep = new File("./" + this.DBName + "/Projects/" + "/" + p.getName() + "File.json");

		filep.delete();
		try {
			objectMapper.writeValue(filep, p);
		} catch (IOException e) {

			///
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param u
	 */
	public void refreshUser(User u) {
		 //TODO ciao provva
		/// controlla null nei parametri

		File fileu = new File("./" + this.DBName + "/Users/" + "/" + u.getnikName() + ".json");

		fileu.delete();
		try {
			objectMapper.writeValue(fileu, u);
		} catch (IOException e) {

			e.printStackTrace();
		}
	}

}


