package com.kschool.samza;

import org.apache.samza.config.Config;
import org.apache.samza.storage.kv.KeyValueStore;
import org.apache.samza.system.IncomingMessageEnvelope;
import org.apache.samza.system.OutgoingMessageEnvelope;
import org.apache.samza.system.SystemStream;
import org.apache.samza.task.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class KschoolStreamTask implements StreamTask, InitableTask, WindowableTask {
    // Logger que escribe dentro del directorio /var/log/samza/
    private static final Logger log = LoggerFactory.getLogger(KschoolStreamTask.class);

    // Stream de salida al topic de Kafka: enrichment
    private final SystemStream OUTPUT_STREAM = new SystemStream("kafka", "enrichment");

    // Cache Location
    KeyValueStore<String, Map<String, Object>> location;

    // Contador de mensajes por minuto
    long count = 0L;


    @Override
    public void init(Config config, TaskContext taskContext) throws Exception {
        /*  Ejercicio 1 - Inicializacion del KeyValueStore denominado location.

                - Para ello utilizaremos la clase taskContext.
         */
        this.location = (KeyValueStore<String, Map<String, Object>>) taskContext.getStore("location");
    }

    public void process(IncomingMessageEnvelope envelope,
                        MessageCollector collector,
                        TaskCoordinator coordinator) {

        /*  Ejercicio 2 - Logica de procesamiento

                - Incremento del contador por cada mensaje.
                - Obtención del mensaje desde el envelope. Tipo del mensaje Map<String, Object>
                - Chequeo de la fuente del mensaje:
                      envelope.getSystemStreamPartition().getSystemStream().getStream();

                      * Si la fuente es loc:

                        - Se almacena la informacion en la cache location, utilizando el campo client como key.
                        - El resto de informacion se almacena como value.

                      * Si la fuente es app:

                        - Se utiliza el cliente consular la cache location, si se obtiene valores se añaden al mensaje.
                        - Se añade al mensaje el contador de eventos por minuto.
                        - Se produce el mensaje utilizando el collector y la clase OutgoingMessageEnvelope.
         */
        String stream = (String) envelope.getSystemStreamPartition().getSystemStream().getStream();

        // String mac = envelope.getKey().toString();
        Map<String, Object> msg = (Map<String, Object>) envelope.getMessage();
        String mac = msg.get("client").toString();

        if (stream.equals("loc")) {

            /*Fuente LOC*/
            count ++;
            Map<String,Object> tmp = new HashMap<String,Object>();
            tmp.put("location",msg.get("location"));
            location.put(mac,tmp);

        } else {
            /*Fuente APP*/
           count ++;
            if (location.get(mac) != null) {
                Map<String,Object> mensaje = location.get(mac);
                HashMap<String,Object> tmp = new HashMap<String,Object>();
                tmp.put("count",count);
                tmp.put("app",msg.get("application"));
                tmp.put("location",mensaje.get("location"));
                collector.send(new OutgoingMessageEnvelope(OUTPUT_STREAM,mac,tmp));

            }

        }

    }

    @Override
    public void window(MessageCollector messageCollector, TaskCoordinator taskCoordinator) throws Exception {
        count = 0L;
    }
}