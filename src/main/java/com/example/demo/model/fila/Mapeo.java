package com.example.demo.model.fila;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
/*
 #REQUISITO Base compartida (reutilizable en Módulo 4)
 Para qué sirve: lectura segura de columnas de la BD. Los números y booleanos devuelven
 null (y no 0/false) cuando la columna es NULL; así no se confunde "vacío" con "cero".
 */
public final class Mapeo {
    private Mapeo() { }

    public static Long largo(ResultSet rs, String c) throws SQLException {
        long v = rs.getLong(c);
        return rs.wasNull() ? null : v;
    }

    public static Integer entero(ResultSet rs, String c) throws SQLException {
        int v = rs.getInt(c);
        return rs.wasNull() ? null : v;
    }

    public static Boolean bool(ResultSet rs, String c) throws SQLException {
        boolean v = rs.getBoolean(c);
        return rs.wasNull() ? null : v;
    }

    public static String texto(ResultSet rs, String c) throws SQLException {
        return rs.getString(c);
    }

    public static LocalDateTime fecha(ResultSet rs, String c) throws SQLException {
        return rs.getObject(c, LocalDateTime.class);
    }

    public static BigDecimal decimal(ResultSet rs, String c) throws SQLException {
        return rs.getBigDecimal(c);
    }
}
