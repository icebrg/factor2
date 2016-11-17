package net.gilstraps.brian.factor2;

public class Animal2 {
    private String name;
    private String home;

    public Animal2(final String name, final String home ) {
        this.home = home;
        this.name = name;
    }

    public String getHome() {
        return home;
    }

    public void setHome(final String home) {
        this.home = home;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
