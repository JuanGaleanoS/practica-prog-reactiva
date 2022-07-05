package com.springboot.reactor.springbootreactor;

import com.springboot.reactor.springbootreactor.model.Comentarios;
import com.springboot.reactor.springbootreactor.model.Usuario;
import com.springboot.reactor.springbootreactor.model.UsuarioComentarios;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

@SpringBootApplication
public class SpringBootReactorApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SpringBootReactorApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SpringBootReactorApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        ejemploContraPresion();
    }

    public void ejemploContraPresion() {

        // Función para probar cuánto límite de datos se puede enviar al request de un observable, por defecto unbounded

        Flux.range(1, 10)
                .log() // Ver traza del flujo
                .limitRate(2)
                .subscribe(
                        /*new Subscriber<Integer>() {

                    private Subscription subscription;
                    private final Integer limite = 5;
                    private Integer consumido = 0;

                    @Override
                    public void onSubscribe(Subscription subscription) {
                        this.subscription = subscription;
                        subscription.request(limite); // límite de datos enviados
                    }

                    @Override
                    public void onNext(Integer integer) {

                        log.info(integer.toString());
                        consumido++;

                        if (consumido.equals(limite)) {
                            consumido = 0;
                            subscription.request(limite); // Se realiza nuevamente el request para recibir más datos
                        }

                    }

                    @Override
                    public void onError(Throwable throwable) {

                    }

                    @Override
                    public void onComplete() {

                    }
                }*/);
    }

    public void ejemploIntervalDesdeCreate() {
        Flux.create(emmiter -> {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                private Integer contador = 0;

                @Override
                public void run() {

                    emmiter.next(++contador); // Elementos que llegan al flux

                    if (contador == 10) {
                        timer.cancel();
                        emmiter.complete();
                    }

                    if (contador == 5) {
                        timer.cancel();
                        emmiter.error(new InterruptedException("Error al llegar a 5"));
                    }
                }
            }, 1000, 1000);
        }).subscribe(
                o -> log.info(o.toString()), // Éxito, elemento a elemento
                throwable -> log.error(throwable.getMessage()), // Error
                () -> log.info("Finalizado") // Al completar
        );
    }

    public void ejemploIntervalInfinito() throws InterruptedException {

        // Para poder pausar el hilo y ver el subscribe
        CountDownLatch latch = new CountDownLatch(1); // Contador que decrementa, al llegar a cero libera el hilo.

        Flux.interval(Duration.ofSeconds(1)) // Emite un valor long cada determinado tiempo
                .flatMap(aLong -> {
                    if (aLong >= 5) {
                        return Flux.error(new RuntimeException("No mayor o igual a 5")); // Obliga a que el flujo termine
                    }
                    return Flux.just(aLong);
                })
                .map(aLong -> "Hola " + aLong)
                .retry(2) // Operador que permite reintentar el flujo cada que falle
                //.doOnNext(log::info)
                .doOnTerminate(latch::countDown) // Al terminar el flujo se baja el contador a cero, haciendo que el hilo se libere. Se ejecuta así falle o no falle el observable
                .subscribe(log::info, throwable -> log.error(throwable.getMessage()));

        latch.await(); // Causes the current thread to wait until the latch has counted down to zero, unless the thread is interrupted
    }

    public void ejemploDelayElements() throws InterruptedException {
        //Emitir una serie de números con delay
        Flux<Integer> rango = Flux.range(1, 12)
                .delayElements(Duration.ofSeconds(2))
                .doOnNext(integer -> log.info(integer.toString()));

        rango.subscribe();
        // También para visualizarlo se puede pausar el hilo durante el tiempo que se mostrarán los elementos
        Thread.sleep(13000);
    }

    public void ejemploInterval() {
        //Emitir una serie de números con delay
        Flux<Integer> rango = Flux.range(1, 12);
        Flux<Long> retraso = Flux.interval(Duration.ofSeconds(2));

        // Al establecer un tiempo de delay y hacer un subscribe la ejecución sucede pero en un segundo plano. Ejecución en paralelo
        // Se está ejecutand en un hilo distinto
        rango.zipWith(retraso, (numeros, tiempo) -> numeros)
                .doOnNext(integer -> log.info(integer.toString()))
                //.subscribe(); sucede en otro hilo, debido al delay
                .blockLast(); // se subcribe para visualizarlo, bloquear hasta que se haya emitido el último elemento
        // hace que se corra todos en un solo hilo
    }

    public void ejemploZipWithRangos() {

        Flux<Integer> integerFlux = Flux.range(0, 4); // Observable de enteros

        Flux.just(1, 2, 3, 4)
                .map(integer -> integer * 2)
                // Para el zipWith La cantidad de elementos que recibe el fujoOther debe ser igual al del flujoSource
                // De lo contrario los resultados se limitarán a la cantidad de elementos del flujoOther
                // En el caso contrario el flujoOther se limitaría a la cantidad de elementos del flujoSoruce
                .zipWith(integerFlux, (integer, integer2) -> String.format("Primer flux: %d, Segundo flux: %d", integer, integer2))
                .subscribe(log::info);
    }

    public void ejemploUsuarioComentariosZipWith2() {
        // Mezcla de streams con flatMap
        Mono<Usuario> usuarioMono = Mono.fromCallable(() -> new Usuario("Juan", "Galeano"));

        Mono<Comentarios> comentariosUsuarioMono = Mono.fromCallable(() -> {
            Comentarios comentarios = new Comentarios();
            comentarios.addComentario("Hola!!");
            comentarios.addComentario("Mañana es pa la playa");
            comentarios.addComentario("Cómo sería");
            return comentarios;
        });

        Mono<UsuarioComentarios> usuarioComentariosMono = usuarioMono
                //Retorna un observable Mono de Tuple2<usuarioMono, comentariosUsuarioMono>
                .zipWith(comentariosUsuarioMono)
                .map(tuple2 -> {
                    Usuario usuario = tuple2.getT1(); // Se obtiene el monoSource
                    Comentarios comentarios = tuple2.getT2(); // Se obtiene el monoOther
                    return new UsuarioComentarios(usuario, comentarios);
                });
        usuarioComentariosMono.subscribe(usuarioComentarios -> log.info(usuarioComentarios.toString()));
    }

    public void ejemploUsuarioComentariosZipWith() {
        // Mezcla de streams con flatMap
        Mono<Usuario> usuarioMono = Mono.fromCallable(() -> new Usuario("Juan", "Galeano"));

        Mono<Comentarios> comentariosUsuarioMono = Mono.fromCallable(() -> {
            Comentarios comentarios = new Comentarios();
            comentarios.addComentario("Hola!!");
            comentarios.addComentario("Mañana es pa la playa");
            comentarios.addComentario("Cómo sería");
            return comentarios;
        });

        Mono<UsuarioComentarios> usuarioComentariosMono = usuarioMono
                //Retorna un observable Mono de UsuarioComentarios
                .zipWith(comentariosUsuarioMono, UsuarioComentarios::new); // el segundo parámetro del zip with es una lambda con parámetros (monoSource, monoOther)
        usuarioComentariosMono.subscribe(usuarioComentarios -> log.info(usuarioComentarios.toString()));
    }

    public void ejemploUsuarioComentariosFlatMap() {
        // Mezcla de streams (observables de distinto tipo) con flatMap
        Mono<Usuario> usuarioMono = Mono.fromCallable(() -> new Usuario("Juan", "Galeano")); //Mono.just(usuario)

        Mono<Comentarios> comentariosUsuarioMono = Mono.fromCallable(() -> {
            Comentarios comentarios = new Comentarios();
            comentarios.addComentario("Hola!!");
            comentarios.addComentario("Mañana es pa la playa");
            comentarios.addComentario("Cómo sería");
            return comentarios;
        });

        usuarioMono.flatMap(usuario -> comentariosUsuarioMono.map(comentarios -> new UsuarioComentarios(usuario, comentarios))) // Se retorna un observable de UsuarioComentarios
                .subscribe(usuarioComentarios -> log.info(usuarioComentarios.toString()));
    }

    public void ejemploCollectList() throws Exception {
        //Retornar un Mono de ArrayList

        List<Usuario> usuariosList = new ArrayList<>();

        usuariosList.add(new Usuario("Juan", "Galeano"));
        usuariosList.add(new Usuario("Anderson", "Parra"));
        usuariosList.add(new Usuario("Christian", "Fernández"));
        usuariosList.add(new Usuario("Esteban", "Vargas"));
        usuariosList.add(new Usuario("Jesús", "Rivera"));

        Flux.fromIterable(usuariosList)// Hasta este punto se está retornando un Flux (con varios datos)
                .collectList() // Se toma el Flux de iterable y se retorna un Mono para conservar todos en un solo arreglo y no iterarlos. Al subscribe llega el arreglo
                // En caso de no utilizar collectList al subscribe llegará uno a uno los elementos del iterable
                .subscribe(usuarioList -> usuarioList.forEach(usuario -> log.info(usuario.toString())));
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
                .flatMap(nombre -> { // Recibe un flux String y Retorna un observable Mono String
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

        //fromIterable toma en cuenta la adición de cada elemento al iterable
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
