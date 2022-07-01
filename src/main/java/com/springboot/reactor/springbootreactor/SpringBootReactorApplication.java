package com.springboot.reactor.springbootreactor;

import com.springboot.reactor.springbootreactor.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class SpringBootReactorApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SpringBootReactorApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SpringBootReactorApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        ejemploCollectList();
    }

    public void ejemploCollectList() throws Exception {
        //Retornar un Mono de ArrayList

        // Se utiliza para convertir de un observable a otro observable (flux o mono), y no desde un tipo de dato normal.

        List<Usuario> usuariosList = new ArrayList<>();

        usuariosList.add(new Usuario("Juan", "Galeano"));
        usuariosList.add(new Usuario("Anderson", "Parra"));
        usuariosList.add(new Usuario("Christian", "Fernández"));
        usuariosList.add(new Usuario("Esteban", "Vargas"));
        usuariosList.add(new Usuario("Jesús", "Rivera"));

        Flux.fromIterable(usuariosList)// Hasta este punto se está retornando un Flux (varios datos)
                .collectList() // Se toma el Flux y se retorna un Mono para agrupar todos en un solo arreglo
                .subscribe(usuarioList -> log.info(usuarioList.toString()));
    }

    public void ejemploToString() throws Exception {

        // Se utiliza para convertir de un observable a otro observable (flux o mono), y no desde un tipo de dato normal.

        List<Usuario> usuariosList = new ArrayList<>();

        usuariosList.add(new Usuario("Juan", "Galeano"));
        usuariosList.add(new Usuario("Anderson", "Parra"));
        usuariosList.add(new Usuario("Christian", "Fernández"));
        usuariosList.add(new Usuario("Esteban", "Vargas"));
        usuariosList.add(new Usuario("Jesús", "Rivera"));

        Flux.fromIterable(usuariosList)
                // Hago que retorne un String a partir de un elemento Usuario
                .map(usuario -> usuario.getNombre().toUpperCase().concat(" ").concat(usuario.getApellido().toUpperCase()))
                .flatMap(nombre -> { // Recibe un String y Retorna un observable String
                    if (nombre.startsWith("A")) {
                        return Mono.just(nombre);
                    }
                    return Mono.empty();
                })
                .map(String::toLowerCase)
                .subscribe(log::info);
    }

    public void ejemploFlatMap() throws Exception {

        // Se utiliza para convertir de un observable a otro observable (flux o mono), y no desde un tipo de dato normal.

        List<String> usuariosList = new ArrayList<>();

        usuariosList.add("Juan Galeano");
        usuariosList.add("Anderson Parra");
        usuariosList.add("Christian Fernández");
        usuariosList.add("Esteban Vargas");
        usuariosList.add("Jesús Rivera");

        Flux.fromIterable(usuariosList).map(usuarioString -> {
                    String[] usuarioArray = usuarioString.toUpperCase().split(" ");
                    return new Usuario(usuarioArray[0], usuarioArray[1]);
                })
                .flatMap(usuario -> { // Retorna un observable de Usuario
                    if (usuario.getNombre().startsWith("A")) {
                        return Mono.just(usuario);
                    }
                    return Mono.empty();
                })
                .map(usuario -> new Usuario(usuario.getNombre().toLowerCase(), usuario.getApellido().toLowerCase()))
                .subscribe(usuario -> log.info(usuario.toString()));
    }

    public void ejemploIterable() throws Exception {

        List<String> usuariosList = new ArrayList<>();
        usuariosList.add("Juan Galeano");
        usuariosList.add("Anderson Parra");
        usuariosList.add("Christian Fernández");
        usuariosList.add("Esteban Vargas");
        usuariosList.add("Jesús Rivera");
        //Creación de un observable INMUTABLE
        //Clase abstracta flux implementa al publisher (CorePublisher<T> extends Publisher<T>), el cual permite subscribirse
        Flux<String> nombres = Flux.fromIterable(usuariosList);//Flux.just("Juan Galeano", "Anderson Parra", "Christian Fernández", "Esteban Vargas", "Jesús Rivera");
        //Cada que el observable reciba un elemento, éste será impreso en la línea de comandos
        //Del observable podemos escuchar los cambios cuando se recibe o se quitan elementos
        // Se activa cada que el flujo notifica que ha llegado un elemento
        //.doOnNext(element -> System.out.println(element));
        Flux<Usuario> usuarioFlux = nombres.map(usuarioString -> { // ** No confundir, al momento de usar un operador siempre se crea una nueva instancia.
                    String[] usuarioArray = usuarioString.toUpperCase().split(" ");
                    return new Usuario(usuarioArray[0], usuarioArray[1]);
                }) // Se genera una nueva instancia del flux original, pero con los datos modificados. Sucede cada que se recibe un elemento
                .filter(usuario -> usuario.getNombre().startsWith("J")) // Condición que permite filtrar los registros que recibe el observable, haciendo que pasen solo los que cumplan
                .doOnNext(usuario -> {
                    if (usuario == null) {
                        throw new RuntimeException("El nombre no puede ser vacío");
                    }
                    log.info(usuario.toString());
                })
                .map(usuario -> new Usuario(usuario.getNombre().toLowerCase(), usuario.getApellido().toLowerCase())); // Se genera una nueva instancia del flux original, pero con los datos modificados. Sucede cada que se recibe un elemento
        //.doOnNext(System.out::println); // Haciendo uso de referencias de clase


        //Las actividades o acciones del flujo/observable no serán visibles hasta que nos subscribamos a él
        usuarioFlux.subscribe(
                usuario -> log.info(usuario.toString()), // En el momento en que el observable recibe cada elemento
                errorConsumer -> log.error(errorConsumer.getMessage()), // Al lanzar una excepción
                () -> log.info("Ha finalizado la ejecución del observable con éxito.") // Runable Al completar el observable
        );
    }
}
