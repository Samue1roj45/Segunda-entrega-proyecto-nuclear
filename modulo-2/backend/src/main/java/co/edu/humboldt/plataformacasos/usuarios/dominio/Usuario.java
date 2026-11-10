package co.edu.humboldt.plataformacasos.usuarios.dominio;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Usuario del sistema. Las reglas de roles y permisos viven en la entidad
 * (no en servicios sueltos) para que no se puedan saltar.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correo_institucional", nullable = false, unique = true, length = 150)
    private String correoInstitucional;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /** Solo personal (docentes y administrador). Hash BCrypt; nunca la contraseña en claro. */
    @Column(name = "clave_hash", length = 100)
    private String claveHash;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_permiso", joinColumns = @JoinColumn(name = "usuario_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permiso", nullable = false, length = 50)
    private Set<Permiso> permisosAsignados = new HashSet<>();

    /** Requerido por JPA. */
    protected Usuario() {
    }

    private Usuario(String correoInstitucional, String nombreCompleto, Rol rol) {
        this.correoInstitucional = normalizarCorreo(correoInstitucional);
        this.nombreCompleto = requerirTexto(nombreCompleto, "nombreCompleto");
        this.rol = Objects.requireNonNull(rol, "rol");
        this.activo = true;
        this.fechaCreacion = LocalDateTime.now();
    }

    /** RF-03: el docente se crea activo y sin permisos; el administrador los asigna después. */
    public static Usuario nuevoDocente(String correoInstitucional, String nombreCompleto) {
        return new Usuario(correoInstitucional, nombreCompleto, Rol.DOCENTE);
    }

    /** Estudiante activo. Su vinculación a casos y su código de autorización los gestiona el Módulo 3. */
    public static Usuario nuevoEstudiante(String correoInstitucional, String nombreCompleto) {
        return new Usuario(correoInstitucional, nombreCompleto, Rol.ESTUDIANTE);
    }

    /**
     * RF-03: reemplaza los permisos del docente. Solo aplica a docentes y solo
     * con permisos marcados como asignables.
     */
    public void asignarPermisos(Set<Permiso> permisos) {
        Objects.requireNonNull(permisos, "permisos");
        if (rol != Rol.DOCENTE) {
            throw new IllegalStateException("Solo se asignan permisos a usuarios con rol Docente");
        }
        for (Permiso permiso : permisos) {
            if (!permiso.asignableADocente()) {
                throw new IllegalArgumentException("El permiso " + permiso + " no se puede asignar a un docente");
            }
        }
        permisosAsignados.clear();
        permisosAsignados.addAll(permisos);
    }

    /** RF-02: permisos del rol más los asignados individualmente. */
    public Set<Permiso> permisosEfectivos() {
        Set<Permiso> efectivos = EnumSet.noneOf(Permiso.class);
        efectivos.addAll(rol.permisosBase());
        efectivos.addAll(permisosAsignados);
        return Collections.unmodifiableSet(efectivos);
    }

    public boolean tienePermiso(Permiso permiso) {
        return activo && permisosEfectivos().contains(permiso);
    }

    /** Docentes y administrador: inician sesión con contraseña. */
    public boolean esPersonal() {
        return rol == Rol.DOCENTE || rol == Rol.ADMINISTRADOR;
    }

    public static String normalizarCorreo(String correo) {
        return requerirTexto(correo, "correoInstitucional").trim().toLowerCase(Locale.ROOT);
    }

    private static String requerirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio");
        }
        return valor.trim();
    }

    public Long getId() {
        return id;
    }

    public String getCorreoInstitucional() {
        return correoInstitucional;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public Rol getRol() {
        return rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public String getClaveHash() {
        return claveHash;
    }

    public Set<Permiso> getPermisosAsignados() {
        return Collections.unmodifiableSet(permisosAsignados);
    }
}
