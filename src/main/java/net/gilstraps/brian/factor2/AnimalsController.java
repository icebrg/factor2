package net.gilstraps.brian.factor2;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sun.misc.IOUtils;

@RestController
public class AnimalsController {

    private static final Logger LOGGER = Logger.getLogger(AnimalsController.class);

    private final Map<String, List<Animal>> typeToAnimals = new ConcurrentHashMap<>();

    {
        typeToAnimals.put("cats", new ArrayList<>(Arrays.asList(
                new Animal("Garfield", "comics", "Chubby"),
                new Animal("Tom", "cartoons", "Fuzzy"),
                new Animal("Simon's Cat", "internet", "Snuggly")
        )));
        typeToAnimals.put("dogs", new ArrayList<>(Arrays.asList(
                new Animal("Lassie", "tv", "Pretty"),
                new Animal("Pluto", "cartoons", "Mouse-lover"),
                new Animal("Bolt", "movies", "Happy")
        )));
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public List<String> getTypesOfAnimals() {
        return typeToAnimals.keySet().stream().sorted().collect(Collectors.toList());
    }

    @RequestMapping(value = "/{type}", method = RequestMethod.GET)
    public synchronized List<Animal> listAnimalsOfType(@PathVariable String type) throws NotFoundException {
        List<Animal> names = typeToAnimals.get(type);
        if (names == null) {
            throw new NotFoundException("No type '" + type + "'");
        }
        return names;
    }

    @RequestMapping(value = "/{type}/{name}", method = RequestMethod.PUT)
    public synchronized void addAnimal(@PathVariable String type, @PathVariable String name, @RequestParam(required = false) String home, @RequestParam(required = false) String secretName) throws DuplicateNameException {
        List<Animal> animalsOfType = typeToAnimals.get(type);
        synchronized (typeToAnimals) {
            if (animalsOfType == null) {
                animalsOfType = new ArrayList<Animal>();
                typeToAnimals.put(type, animalsOfType);
            }
        }
        if (animalsOfType.stream().anyMatch(a-> name.equals(a.getName()))) {
            throw new DuplicateNameException();
        }
        animalsOfType.add(new Animal(name,home,secretName));
    }

    @RequestMapping(value = "/{type}/{name}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public synchronized Animal getAnimal(@PathVariable String type, @PathVariable String name) throws NotFoundException {
        return typeToAnimals.get(type).stream().filter(a->a.getName().equals(name)).findFirst().orElseThrow(NotFoundException::new);
    }

    @RequestMapping(value = "/{type}/{name}", method = RequestMethod.GET, produces = MediaType.IMAGE_PNG_VALUE)
    public synchronized byte[] getAnimalImage(@PathVariable String type, @PathVariable String name) throws NotFoundException, IOException, InterruptedException {
        // HACK!!
        byte[] scaledVersion = scale( name );
        return scaledVersion;
    }

    // Still a lot long wrong here...
    private byte[] scale(final String resourceName) throws IOException, InterruptedException {
        final InputStream inputStream = Animal.class.getResourceAsStream("/" + resourceName + ".png");
        final BufferedImage image = ImageIO.read(inputStream);
        final double width = image.getWidth();
        final double height = image.getHeight();
        int newWidth;
        int newHeight;
        if ( width > height ) {
            final double scaleFactor = 96 / width;
            newWidth = 96;
            newHeight = (int) Math.round(height * scaleFactor);
        }
        else {
            final double scaleFactor = 96 / height;
            newHeight = 96;
            newWidth = (int) Math.round(width * scaleFactor);
        }
        Image scaled = image.getScaledInstance(newWidth,newHeight, Image.SCALE_SMOOTH);
        BufferedImage bufferedImage = toBufferedImage(scaled);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage,"png",byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    @RequestMapping(value = "/{type}/{name}", method = RequestMethod.DELETE)
    public synchronized void deleteAnimal(@PathVariable String type,
                                          @PathVariable String name) throws DuplicateNameException, NotFoundException {
        List<Animal> animalsOfType = typeToAnimals.get(type);
        if (animalsOfType != null) {
            boolean removed = animalsOfType.removeIf(a->name.equals(a.getName()));
            if ( ! removed ) {
                throw new NotFoundException();
            }
        }
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<NotFoundException> notFound(NotFoundException e) {
        return new ResponseEntity<NotFoundException>(HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DuplicateNameException.class)
    public ResponseEntity<DuplicateNameException> duplicate(DuplicateNameException e) {
        // For now, we don't allow over-writing an existing animal, just for example purposes.
        return new ResponseEntity<DuplicateNameException>(HttpStatus.FORBIDDEN);
    }

    public static BufferedImage toBufferedImage(Image img)
    {
        if (img instanceof BufferedImage)
        {
            return (BufferedImage) img;
        }

        // Create a buffered image with transparency
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        // Return the buffered image
        return bimage;
    }
}
