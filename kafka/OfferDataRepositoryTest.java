package com.banamex.cops.update_offer.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OfferDataRepositoryTest {

    @Mock ObjectProvider<CitiDataStore> citiDataStore;
    @Mock ObjectProvider<JdbcTemplate> defaultJdbc;
    @Mock JdbcTemplate jdbc;

    private OfferDataRepository repository;

    @BeforeEach
    void setUp() {
        repository = new OfferDataRepository(citiDataStore, defaultJdbc);
        when(citiDataStore.getIfAvailable()).thenReturn(null);
        when(defaultJdbc.getIfAvailable()).thenReturn(jdbc);
    }

    private void filas(List<Map<String, Object>> rows) {
        when(jdbc.query(any(PreparedStatementCreator.class), ArgumentMatchers.<RowMapper<Map<String, Object>>>any()))
                .thenReturn(rows);
    }

    // ------------------------------------------------------------ SQL

    @Test
    void sql_traeLas100ColumnasDeUserFieldYLasTablasDelModelo() {
        String sql = OfferDataRepository.buildSql();

        assertThat(sql.split("OFFPAL\\.USERFIELD_", -1).length - 1).isEqualTo(100);
        assertThat(sql).contains("OFFPAL.USERFIELD_1,", "OFFPAL.USERFIELD_100",
                "D_OFFERPALETTE OFFPAL", "D_CAMPAIGN CAMP", "D_PRODUCT PRD", "D_WAVECHINFO WAVCH",
                "AS APPTENDTIMEDUR", "WAVCH.EXTID$ = ? AND OFFPAL.EXTID$ = ?",
                "OFFPAL.campaignid = CAMP.cmpid", "CAMP.PRDCODE = PRD.PRDCODE");
    }

    @Test
    void sql_usaDosParametrosPreparados() {
        String sql = OfferDataRepository.buildSql();
        assertThat(sql.chars().filter(c -> c == '?').count()).isEqualTo(2);
    }

    // ------------------------------------------------------------ consulta

    @Test
    void conUnaFila_laDevuelve() {
        filas(List.of(Map.of("PRDTYPE", "T")));
        Optional<Map<String, Object>> r = repository.find("wvch:W1:CAT", "off:mx:C1:W1:123");
        assertThat(r).isPresent();
        assertThat(r.get()).containsEntry("PRDTYPE", "T");
    }

    @Test
    void sinFilas_devuelveVacio() {
        filas(List.of());
        assertThat(repository.find("a", "b")).isEmpty();
    }

    @Test
    void conVariasFilas_usaLaPrimera() {
        filas(List.of(Map.of("N", 1), Map.of("N", 2)));
        assertThat(repository.find("a", "b").get()).containsEntry("N", 1);
    }

    @Test
    void creaLaSentenciaConTimeoutMaximoDeFilasYParametros() throws Exception {
        filas(List.of());
        repository.find("wvch:W1:CAT", "off:mx:C1:W1:123");

        ArgumentCaptor<PreparedStatementCreator> c = ArgumentCaptor.forClass(PreparedStatementCreator.class);
        verify(jdbc).query(c.capture(), ArgumentMatchers.<RowMapper<Map<String, Object>>>any());

        Connection con = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(con.prepareStatement(anyString())).thenReturn(ps);
        c.getValue().createPreparedStatement(con);

        verify(ps).setQueryTimeout(10);
        verify(ps).setMaxRows(100);
        verify(ps).setString(1, "wvch:W1:CAT");
        verify(ps).setString(2, "off:mx:C1:W1:123");
    }

    // ------------------------------------------------------------ qué conexión usa

    @Test
    void conConexionDedicada_laPrefiereSobreLaPrincipal() {
        CitiDataStore dedicated = mock(CitiDataStore.class);
        JdbcTemplate dedicatedJdbc = mock(JdbcTemplate.class);
        when(citiDataStore.getIfAvailable()).thenReturn(dedicated);
        when(dedicated.getJdbcTemplate()).thenReturn(dedicatedJdbc);
        when(dedicatedJdbc.query(any(PreparedStatementCreator.class),
                ArgumentMatchers.<RowMapper<Map<String, Object>>>any())).thenReturn(List.of(Map.of("X", 1)));

        assertThat(repository.find("a", "b")).isPresent();
        verify(defaultJdbc, never()).getIfAvailable();
    }

    @Test
    void sinNingunaConexion_lanzaIllegalState() {
        when(defaultJdbc.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(() -> repository.find("a", "b"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("datasource");
    }
}
