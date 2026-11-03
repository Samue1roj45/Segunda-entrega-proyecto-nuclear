package com.example.demo.exception;

import java.sql.SQLException;
/*
 #REQUISITO Transversal
 Para qué sirve: ayudas para reconocer los errores que lanza la BD con
 SIGNAL SQLSTATE '45000' (reglas de negocio de triggers y procedimientos).
 */
public final class SqlErrores {

    private SqlErrores() { }

    public static SQLException buscar(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof SQLException s) return s;
        }
        return null;
    }

    public static boolean esReglaDeBd(Throwable t) {
        SQLException s = buscar(t);
        return s != null && "45000".equals(s.getSQLState());
    }
}
