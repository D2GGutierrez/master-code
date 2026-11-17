-- HU45-T3 y HU48-T3: cada respuesta del celular queda como un envío (plataforma movil). Si la señal se corta justo
-- cuando el servidor ya la guardó, la app la reenvía sin saber que llegó: clave_cliente es el identificador único que la
-- app le pone a cada intento, y el índice impide que el mismo intento se registre dos veces (CA-48.5). Es opcional: los
-- envíos de la web y los de antes no la traen.
ALTER TABLE app_schema.app_envio ADD COLUMN clave_cliente VARCHAR(40);

CREATE UNIQUE INDEX ux_app_envio_clave_cliente ON app_schema.app_envio (estudiante_id, clave_cliente)
    WHERE clave_cliente IS NOT NULL;
