-- Nodos de diálogo
UPDATE dialog_nodes SET text = 'El abuelo se detiene en medio del camino y te mira fijamente. "¿Sabes qué hacer si te encuentras un dragón en el bosque? Vamos a comprobarlo."' WHERE id = 'trial_start';
UPDATE dialog_nodes SET text = '¡Un RUGIDO sacude los árboles! El dragón aparece por la DERECHA y se abalanza sobre ti. ¿Qué haces?' WHERE id = 'trial_q1';
UPDATE dialog_nodes SET text = '¡El dragón escupe fuego! Una llamarada viene directa hacia tu cara. ¿Qué haces?' WHERE id = 'trial_q2';
UPDATE dialog_nodes SET text = 'El dragón te bloquea el paso y te mira a los ojos. No hay escapatoria. ¿Qué haces?' WHERE id = 'trial_q3';
UPDATE dialog_nodes SET text = 'El dragón se detiene. El bosque queda en silencio. El abuelo te observa con una sonrisa...' WHERE id = 'trial_evaluation_node';
UPDATE dialog_nodes SET text = 'El abuelo aplaude despacio. "¡Bien hecho! Eso es exactamente lo que haría un héroe. Los dragones respetan a quien no les teme." El dragón, sorprendido, desaparece entre la niebla.' WHERE id = 'trial_success';
UPDATE dialog_nodes SET text = 'El abuelo te revuelve el pelo con cariño. "Bueno... técnicamente el dragón te engulló. Pero te escupió porque le diste acidez estomacal. ¡También es una victoria a su manera!"' WHERE id = 'trial_fail';

-- Opciones de la pregunta 1
UPDATE dialog_options SET text = '[<-] Esquivar hacia la izquierda' WHERE node_id = 'trial_q1' AND score_value = 1;
UPDATE dialog_options SET text = '[->] Quedarte quieto' WHERE node_id = 'trial_q1' AND score_value = 0;

-- Opciones de la pregunta 2
UPDATE dialog_options SET text = '[v] Tirarte al suelo' WHERE node_id = 'trial_q2' AND score_value = 1;
UPDATE dialog_options SET text = '[^] Saltar hacia arriba' WHERE node_id = 'trial_q2' AND score_value = 0;

-- Opciones de la pregunta 3
UPDATE dialog_options SET text = '[->] Mirarlo a los ojos sin miedo' WHERE node_id = 'trial_q3' AND score_value = 1;
UPDATE dialog_options SET text = '[<-] Echar a correr' WHERE node_id = 'trial_q3' AND score_value = 0;
