package net.gilstraps.brian.factor2;

public class Animal {
    private String name;
    private String secretName;
    private String home;

    public Animal(final String name,final String home, final String secretName) {
        this.home = home;
        this.name = name;
        this.secretName = secretName;
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

    public String getSecretName() {
        return secretName;
    }

    public void setSecretName(final String secretName) {
        this.secretName = secretName;
    }
}
