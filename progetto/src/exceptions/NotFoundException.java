package exceptions;

public class NotFoundException extends Exception {
	private static final long serialVersionUID = 1L;

    public NotFoundException() {

        super();
    }

    public NotFoundException(String s) {

        super(s);
    }
}
