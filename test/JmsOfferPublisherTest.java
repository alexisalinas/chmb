package com.banamex.cops.update_offer.kafka;

import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JmsOfferPublisherTest {

    @Mock JmsTemplate jms;

    private final JsonMapper mapper = JsonMapper.builder().build();
    private JmsOfferPublisher publisher;
    private Session session;
    private TextMessage message;

    @BeforeEach
    void setUp() throws Exception {
        OfferKafkaProps props = new OfferKafkaProps();
        props.setQueue("cola.bridge");
        publisher = new JmsOfferPublisher(jms, props, mapper);

        session = mock(Session.class);
        message = mock(TextMessage.class);
        when(session.createTextMessage(anyString())).thenReturn(message);
        when(message.getJMSMessageID()).thenReturn("ID:123");

        doAnswer(inv -> {
            MessageCreator creator = inv.getArgument(1);
            creator.createMessage(session);
            return null;
        }).when(jms).send(eq("cola.bridge"), any(MessageCreator.class));
    }

    @Test
    void enviaElEnvoltorioRefIdYJsonStringALaCola() throws Exception {
        publisher.publish("ref1", "{\"event\":{}}");

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(session).createTextMessage(body.capture());
        JsonNode envelope = mapper.readTree(body.getValue());
        assertThat(envelope.get("RefId").asText()).isEqualTo("ref1");
        assertThat(envelope.get("jsonString").asText()).isEqualTo("{\"event\":{}}");
    }

    @Test
    void poneElRefIdComoCorrelationId() throws Exception {
        publisher.publish("ref1", "{}");
        verify(message).setJMSCorrelationID("ref1");
    }

    @Test
    void devuelveElMessageIdAsignadoPorElDestino() throws Exception {
        assertThat(publisher.publish("ref1", "{}")).isEqualTo("ID:123");
    }

    @Test
    void siFallaElEnvio_propagaLaExcepcionParaQueSeReintente() {
        doThrow(new org.springframework.jms.UncategorizedJmsException("broker caído"))
                .when(jms).send(eq("cola.bridge"), any(MessageCreator.class));

        assertThatThrownBy(() -> publisher.publish("ref1", "{}"))
                .isInstanceOf(org.springframework.jms.JmsException.class);
    }
}
