package exceptions;

public class MissingFolderException extends Exception{
	private static final long serialVersionUID = 1L;

    public MissingFolderException() {

        super();
    }

    public MissingFolderException(String s) {

        super(s);
    }

}
