-- HU40-T4: el mensaje que se le mostró al estudiante cuando su código falló (no compiló, se pasó de tiempo o de
-- memoria, o falló al ejecutarse), guardado con el envío para verlo después en el historial. Es NULL cuando el envío
-- no tuvo ningún error de ese tipo (pasó o solo dio una respuesta distinta) y en los envíos anteriores a esta migración.
ALTER TABLE app_schema.app_envio ADD COLUMN mensaje_error TEXT;
