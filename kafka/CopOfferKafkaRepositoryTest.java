package com.banamex.cops.app.repository;

import com.banamex.cops.app.model.CopOfferKafkaData;
import com.banamex.cops.app.natives.CopOfferKafkaNatives;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CopOfferKafkaRepositoryTest {

    @Mock NamedParameterJdbcTemplate jdbc;
    @Mock JdbcOperations ops;

    private CopOfferKafkaRepository repository;

    @BeforeEach
    void setUp() {
        repository = new CopOfferKafkaRepository(jdbc);
        when(jdbc.getJdbcOperations()).thenReturn(ops);
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<RowMapper<CopOfferKafkaData>> mapperCaptor() {
        return ArgumentCaptor.forClass(RowMapper.class);
    }

    @Test
    void devuelveLasFilasDelJdbc() {
        CopOfferKafkaData fila = new CopOfferKafkaData("C", "T", "R", "D", "P", "F", List.of(),
                null, "30", null, null, "PC", "PF", "Y");
        when(ops.query(any(PreparedStatementCreator.class), ArgumentMatchers.<RowMapper<CopOfferKafkaData>>any()))
                .thenReturn(List.of(fila));

        assertThat(repository.findByWaveKeyAndOfferKey("wvch:W1:CAT", "off:mx:C1:W1:123")).containsExactly(fila);
    }

    @Test
    void sinFilasDevuelveListaVacia() {
        when(ops.query(any(PreparedStatementCreator.class), ArgumentMatchers.<RowMapper<CopOfferKafkaData>>any()))
                .thenReturn(List.of());
        assertThat(repository.findByWaveKeyAndOfferKey("a", "b")).isEmpty();
    }

    @Test
    void creaLaSentenciaConTimeoutMaximoDeFilasYParametrosEnOrden() throws Exception {
        repository.findByWaveKeyAndOfferKey("wvch:W1:CAT", "off:mx:C1:W1:123");

        ArgumentCaptor<PreparedStatementCreator> c = ArgumentCaptor.forClass(PreparedStatementCreator.class);
        verify(ops).query(c.capture(), ArgumentMatchers.<RowMapper<CopOfferKafkaData>>any());

        Connection con = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(con.prepareStatement(anyString())).thenReturn(ps);
        c.getValue().createPreparedStatement(con);

        verify(con).prepareStatement(CopOfferKafkaNatives.FIND_BY_WAVE_KEY_AND_OFFER_KEY);
        verify(ps).setQueryTimeout(10);
        verify(ps).setMaxRows(100);
        verify(ps).setString(1, "wvch:W1:CAT");
        verify(ps).setString(2, "off:mx:C1:W1:123");
    }

    @Test
    void mapeaLasColumnasAlRecord() throws Exception {
        repository.findByWaveKeyAndOfferKey("a", "b");
        ArgumentCaptor<RowMapper<CopOfferKafkaData>> mapper = mapperCaptor();
        verify(ops).query(any(PreparedStatementCreator.class), mapper.capture());

        Timestamp start = Timestamp.valueOf("2026-10-25 10:30:00");
        Timestamp expiry = Timestamp.valueOf("2026-12-31 23:59:59");
        Timestamp created = Timestamp.valueOf("2026-01-01 00:00:00");
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("CRPCATEGORYID")).thenReturn("CAT1");
        when(rs.getString("PRDTYPE")).thenReturn("T");
        when(rs.getString("REASONCODE")).thenReturn("R1");
        when(rs.getString("REASONDESC")).thenReturn("motivo");
        when(rs.getString("DESCRIPTION")).thenReturn("producto");
        when(rs.getString("FULFILLMENTPROCID")).thenReturn("FP1");
        when(rs.getString("USERFIELD_1")).thenReturn("u1");
        when(rs.getString("USERFIELD_50")).thenReturn("u50");
        when(rs.getString("USERFIELD_100")).thenReturn("u100");
        when(rs.getTimestamp("APPTSTARTDATE_TM")).thenReturn(start);
        when(rs.getString("APPTENDTIMEDUR")).thenReturn("30");
        when(rs.getTimestamp("EXPIRYDATE_TM")).thenReturn(expiry);
        when(rs.getTimestamp("OFFERCREATIONDATE_TM")).thenReturn(created);
        when(rs.getString("PRDCODE")).thenReturn("P1");
        when(rs.getString("PRDFAMILY")).thenReturn("F1");
        when(rs.getString("AVAILABILITY")).thenReturn("Y");

        CopOfferKafkaData r = mapper.getValue().mapRow(rs, 1);

        assertThat(r.crpCategoryId()).isEqualTo("CAT1");
        assertThat(r.prdType()).isEqualTo("T");
        assertThat(r.reasonCode()).isEqualTo("R1");
        assertThat(r.reasonDesc()).isEqualTo("motivo");
        assertThat(r.prdDescription()).isEqualTo("producto");
        assertThat(r.fulfillmentProcId()).isEqualTo("FP1");
        assertThat(r.apptStartDate()).isEqualTo(start);
        assertThat(r.apptEndTimeDur()).isEqualTo("30");
        assertThat(r.expiryDate()).isEqualTo(expiry);
        assertThat(r.offerCreationDate()).isEqualTo(created);
        assertThat(r.prdCode()).isEqualTo("P1");
        assertThat(r.prdFamily()).isEqualTo("F1");
        assertThat(r.availability()).isEqualTo("Y");
    }

    @Test
    void losUserFieldsVienenEnOrdenIndice0EsElUno() throws Exception {
        repository.findByWaveKeyAndOfferKey("a", "b");
        ArgumentCaptor<RowMapper<CopOfferKafkaData>> mapper = mapperCaptor();
        verify(ops).query(any(PreparedStatementCreator.class), mapper.capture());

        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("USERFIELD_1")).thenReturn("u1");
        when(rs.getString("USERFIELD_50")).thenReturn("u50");
        when(rs.getString("USERFIELD_100")).thenReturn("u100");

        List<String> uf = mapper.getValue().mapRow(rs, 1).userFields();

        assertThat(uf).hasSize(100);
        assertThat(uf.get(0)).isEqualTo("u1");
        assertThat(uf.get(49)).isEqualTo("u50");
        assertThat(uf.get(99)).isEqualTo("u100");
        assertThat(uf.get(1)).isNull();
    }
}
