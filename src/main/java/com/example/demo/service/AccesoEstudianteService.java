package com.example.demo.service;

import com.example.demo.dto.AccesoEstudianteResponse;
import com.example.demo.exception.NoAutenticadoException;
import com.example.demo.exception.SqlErrores;
import com.example.demo.repository.AccesoRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
/*
 #REQUISITO RF-20
 Para qué sirve: el estudiante entra con correo institucional + código de autorización.
 Si la BD rechaza (correo o código inválidos, vencidos, sesión finalizada) se responde
 401 con el mensaje de la BD.
 */
@Service
public class AccesoEstudianteService {

    private final AccesoRepository repo;

    public AccesoEstudianteService(AccesoRepository repo) {
        this.repo = repo;
    }

    public AccesoEstudianteResponse autenticar(String correo, String codigo) {
        try {
            return repo.autenticarEstudiante(correo.trim(), codigo.trim().toUpperCase());
        } catch (DataAccessException e) {
            if (SqlErrores.esReglaDeBd(e)) {
                throw new NoAutenticadoException(SqlErrores.buscar(e).getMessage());
            }
            throw e;
        }
    }
}
