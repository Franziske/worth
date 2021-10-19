package exceptions;

public class SuchDBAlreadyExistsException extends Exception{
	private static final long serialVersionUID = 1L;

    public SuchDBAlreadyExistsException() {

        super();
    }

    public SuchDBAlreadyExistsException(String s) {

        super(s);
    }
}
