package net.gilstraps.brian.factor2;

public class DuplicateNameException extends Exception {
    public DuplicateNameException() {
    }

    public DuplicateNameException(String s) {
        super(s);
    }

    public DuplicateNameException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public DuplicateNameException(Throwable throwable) {
        super(throwable);
    }
}
