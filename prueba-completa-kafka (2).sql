-- ============================================================================
-- PRUEBA COMPLETA DE LA CONSULTA DE KAFKA (oferta real de UAT)
-- Corre cada bloque por separado (Ejecutar sentencia, F9) con la conexion UAT_COPS.
-- Cambia los tres valores del bloque WITH par si quieres probar otra oferta.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- BLOQUE 1: RESUMEN. Una sola fila que dice que existe y que no.
--   oferta_existe, campana_existe, producto_existe  -> deben ser 1 (o mas)
--   olas_encontradas                                -> debe ser 1 (o mas)
--   olas_con_formato_del_micro                      -> debe ser igual a olas_encontradas
--                                                      (el micro busca 'wvch:OLA:CANAL')
--   clave_de_ola_real, canal                        -> la clave real de la ola y el valor de "channelCode"
-- ----------------------------------------------------------------------------
WITH par AS (
  SELECT 'off:mx:DYNMCBIRTHWELC:DUNIUWPEMQ:023810810' AS offer_key,
         'DYNMCBIRTHWELC' AS campana,
         'DUNIUWPEMQ'     AS ola
    FROM dual
)
SELECT
  (SELECT COUNT(*) FROM D_OFFERPALETTE
    WHERE D_OFFERPALETTE.EXTID$ = par.offer_key)                                        AS oferta_existe,
  (SELECT COUNT(*) FROM D_CAMPAIGN
    WHERE D_CAMPAIGN.CMPID = par.campana)                                                AS campana_existe,
  (SELECT COUNT(*) FROM D_PRODUCT
    WHERE D_PRODUCT.PRDCODE IN (SELECT D_CAMPAIGN.PRDCODE FROM D_CAMPAIGN
                                 WHERE D_CAMPAIGN.CMPID = par.campana))                  AS producto_existe,
  (SELECT COUNT(*) FROM D_WAVECHINFO
    WHERE D_WAVECHINFO.EXTID$ LIKE '%' || par.ola || '%')                                AS olas_encontradas,
  (SELECT COUNT(*) FROM D_WAVECHINFO
    WHERE D_WAVECHINFO.EXTID$ LIKE 'wvch:' || par.ola || ':%')                           AS olas_con_formato_del_micro,
  (SELECT MIN(D_WAVECHINFO.EXTID$) FROM D_WAVECHINFO
    WHERE D_WAVECHINFO.EXTID$ LIKE '%' || par.ola || '%')                                AS clave_de_ola_real,
  (SELECT MIN(D_WAVECHINFO.CHANNELID) FROM D_WAVECHINFO
    WHERE D_WAVECHINFO.EXTID$ LIKE '%' || par.ola || '%')                                AS canal
  FROM par;

-- ----------------------------------------------------------------------------
-- BLOQUE 2: LA CONSULTA COMPLETA, igual a la de la libreria (4 tablas, los 100 USERFIELD y todas las
-- columnas). La clave de la ola se obtiene sola con una subconsulta.
--   - Si devuelve una fila: todas las tablas y columnas existen y la oferta trae datos.
--   - Si falla con ORA-00904: el mensaje dice la columna que no existe.
--   - Si falla con ORA-00942: falta una tabla (o tu usuario no la ve).
--   - Si devuelve cero filas: usa el bloque 1 para ver que pieza no existe.
-- ----------------------------------------------------------------------------
SELECT
       D_WAVECHINFO.CRPCATEGORYID, D_CAMPAIGN.PRDTYPE, D_OFFERPALETTE.REASONCODE, D_OFFERPALETTE.REASONDESC,
       D_PRODUCT.DESCRIPTION, D_CAMPAIGN.FULFILLMENTPROCID, D_OFFERPALETTE.USERFIELD_1, D_OFFERPALETTE.USERFIELD_2,
       D_OFFERPALETTE.USERFIELD_3, D_OFFERPALETTE.USERFIELD_4, D_OFFERPALETTE.USERFIELD_5, D_OFFERPALETTE.USERFIELD_6,
       D_OFFERPALETTE.USERFIELD_7, D_OFFERPALETTE.USERFIELD_8, D_OFFERPALETTE.USERFIELD_9, D_OFFERPALETTE.USERFIELD_10,
       D_OFFERPALETTE.USERFIELD_11, D_OFFERPALETTE.USERFIELD_12, D_OFFERPALETTE.USERFIELD_13, D_OFFERPALETTE.USERFIELD_14,
       D_OFFERPALETTE.USERFIELD_15, D_OFFERPALETTE.USERFIELD_16, D_OFFERPALETTE.USERFIELD_17, D_OFFERPALETTE.USERFIELD_18,
       D_OFFERPALETTE.USERFIELD_19, D_OFFERPALETTE.USERFIELD_20, D_OFFERPALETTE.USERFIELD_21, D_OFFERPALETTE.USERFIELD_22,
       D_OFFERPALETTE.USERFIELD_23, D_OFFERPALETTE.USERFIELD_24, D_OFFERPALETTE.USERFIELD_25, D_OFFERPALETTE.USERFIELD_26,
       D_OFFERPALETTE.USERFIELD_27, D_OFFERPALETTE.USERFIELD_28, D_OFFERPALETTE.USERFIELD_29, D_OFFERPALETTE.USERFIELD_30,
       D_OFFERPALETTE.USERFIELD_31, D_OFFERPALETTE.USERFIELD_32, D_OFFERPALETTE.USERFIELD_33, D_OFFERPALETTE.USERFIELD_34,
       D_OFFERPALETTE.USERFIELD_35, D_OFFERPALETTE.USERFIELD_36, D_OFFERPALETTE.USERFIELD_37, D_OFFERPALETTE.USERFIELD_38,
       D_OFFERPALETTE.USERFIELD_39, D_OFFERPALETTE.USERFIELD_40, D_OFFERPALETTE.USERFIELD_41, D_OFFERPALETTE.USERFIELD_42,
       D_OFFERPALETTE.USERFIELD_43, D_OFFERPALETTE.USERFIELD_44, D_OFFERPALETTE.USERFIELD_45, D_OFFERPALETTE.USERFIELD_46,
       D_OFFERPALETTE.USERFIELD_47, D_OFFERPALETTE.USERFIELD_48, D_OFFERPALETTE.USERFIELD_49, D_OFFERPALETTE.USERFIELD_50,
       D_OFFERPALETTE.USERFIELD_51, D_OFFERPALETTE.USERFIELD_52, D_OFFERPALETTE.USERFIELD_53, D_OFFERPALETTE.USERFIELD_54,
       D_OFFERPALETTE.USERFIELD_55, D_OFFERPALETTE.USERFIELD_56, D_OFFERPALETTE.USERFIELD_57, D_OFFERPALETTE.USERFIELD_58,
       D_OFFERPALETTE.USERFIELD_59, D_OFFERPALETTE.USERFIELD_60, D_OFFERPALETTE.USERFIELD_61, D_OFFERPALETTE.USERFIELD_62,
       D_OFFERPALETTE.USERFIELD_63, D_OFFERPALETTE.USERFIELD_64, D_OFFERPALETTE.USERFIELD_65, D_OFFERPALETTE.USERFIELD_66,
       D_OFFERPALETTE.USERFIELD_67, D_OFFERPALETTE.USERFIELD_68, D_OFFERPALETTE.USERFIELD_69, D_OFFERPALETTE.USERFIELD_70,
       D_OFFERPALETTE.USERFIELD_71, D_OFFERPALETTE.USERFIELD_72, D_OFFERPALETTE.USERFIELD_73, D_OFFERPALETTE.USERFIELD_74,
       D_OFFERPALETTE.USERFIELD_75, D_OFFERPALETTE.USERFIELD_76, D_OFFERPALETTE.USERFIELD_77, D_OFFERPALETTE.USERFIELD_78,
       D_OFFERPALETTE.USERFIELD_79, D_OFFERPALETTE.USERFIELD_80, D_OFFERPALETTE.USERFIELD_81, D_OFFERPALETTE.USERFIELD_82,
       D_OFFERPALETTE.USERFIELD_83, D_OFFERPALETTE.USERFIELD_84, D_OFFERPALETTE.USERFIELD_85, D_OFFERPALETTE.USERFIELD_86,
       D_OFFERPALETTE.USERFIELD_87, D_OFFERPALETTE.USERFIELD_88, D_OFFERPALETTE.USERFIELD_89, D_OFFERPALETTE.USERFIELD_90,
       D_OFFERPALETTE.USERFIELD_91, D_OFFERPALETTE.USERFIELD_92, D_OFFERPALETTE.USERFIELD_93, D_OFFERPALETTE.USERFIELD_94,
       D_OFFERPALETTE.USERFIELD_95, D_OFFERPALETTE.USERFIELD_96, D_OFFERPALETTE.USERFIELD_97, D_OFFERPALETTE.USERFIELD_98,
       D_OFFERPALETTE.USERFIELD_99, D_OFFERPALETTE.USERFIELD_100, D_OFFERPALETTE.APPTSTARTDATE_TM, D_OFFERPALETTE.APPTENDDATE_TM,
       D_OFFERPALETTE.EXPIRYDATE_TM, D_OFFERPALETTE.OFFERCREATIONDATE_TM, D_PRODUCT.PRDCODE, D_PRODUCT.PRDFAMILY,
       D_OFFERPALETTE.AVAILABILITY
  FROM D_OFFERPALETTE, D_CAMPAIGN, D_PRODUCT, D_WAVECHINFO
 WHERE D_OFFERPALETTE.EXTID$ = 'off:mx:DYNMCBIRTHWELC:DUNIUWPEMQ:023810810'
   AND D_OFFERPALETTE.CAMPAIGNID = D_CAMPAIGN.CMPID
   AND D_CAMPAIGN.PRDCODE = D_PRODUCT.PRDCODE
   AND D_WAVECHINFO.EXTID$ = (SELECT MIN(D_WAVECHINFO.EXTID$) FROM D_WAVECHINFO
                               WHERE D_WAVECHINFO.EXTID$ LIKE '%' || D_OFFERPALETTE.WAVEID || '%');

-- ----------------------------------------------------------------------------
-- BLOQUE 3: GENERA EL JSON PARA SOAPUI directamente desde la base.
-- Solo trae ofertas cuya ola SI existe en D_WAVECHINFO (las otras darian KAFKA-NO-ROWS).
-- Copia el valor de la columna json_para_soapui y pegalo como cuerpo de la peticion.
-- ----------------------------------------------------------------------------
SELECT D_OFFERPALETTE.EXTID$ AS clave_de_la_oferta,
       '[{"channelCode":"' || (SELECT MIN(D_WAVECHINFO.CHANNELID) FROM D_WAVECHINFO
                                WHERE D_WAVECHINFO.EXTID$ LIKE '%' || D_OFFERPALETTE.WAVEID || '%')
       || '","campaignId":"' || D_OFFERPALETTE.CAMPAIGNID
       || '","waveId":"'     || D_OFFERPALETTE.WAVEID
       || '","customerId":"' || REGEXP_SUBSTR(D_OFFERPALETTE.EXTID$, '[^:]+', 1, 5) || '"'
       || NVL2(REGEXP_SUBSTR(D_OFFERPALETTE.EXTID$, '[^:]+', 1, 6),
               ',"accountNumber":"' || REGEXP_SUBSTR(D_OFFERPALETTE.EXTID$, '[^:]+', 1, 6) || '"', '')
       || NVL2(REGEXP_SUBSTR(D_OFFERPALETTE.EXTID$, '[^:]+', 1, 7),
               ',"cardNumber":"' || REGEXP_SUBSTR(D_OFFERPALETTE.EXTID$, '[^:]+', 1, 7) || '"', '')
       || ',"offerStatus":"H"}]' AS json_para_soapui
  FROM D_OFFERPALETTE
 WHERE EXISTS (SELECT 1 FROM D_WAVECHINFO
                WHERE D_WAVECHINFO.EXTID$ LIKE '%' || D_OFFERPALETTE.WAVEID || '%')
   AND ROWNUM <= 5;
