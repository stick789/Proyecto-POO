-- Crear la base de datos si no existe
CREATE DATABASE IF NOT EXISTS `proyecto_poo`;
USE `proyecto_poo`;

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

-- --------------------------------------------------------
-- 1. TABLAS INDEPENDIENTES (Sin llaves foráneas externas)
-- --------------------------------------------------------

CREATE TABLE `rol` (
  `id_rol` int(11) NOT NULL AUTO_INCREMENT,
  `nombre_rol` varchar(50) NOT NULL,
  `descripcion` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id_rol`),
  UNIQUE KEY `nombre_rol` (`nombre_rol`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `rol` (`nombre_rol`, `descripcion`) VALUES
('USUARIO', 'Usuario regular del sistema'),
('ADMINISTRADOR', 'Administrador del sistema'),
('ENTRENADOR', 'Entrenador del gimnasio o piscina');

CREATE TABLE `persona` (
  `id_persona` int(11) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) NOT NULL,
  `email` varchar(100) NOT NULL,
  `tipodocumento` varchar(100) NOT NULL,
  `numdocumento` varchar(100) NOT NULL,
  PRIMARY KEY (`id_persona`),
  UNIQUE KEY `idx_numdocumento` (`numdocumento`) -- Necesario para la FK de Entrenador
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `instalacion` (
  `idInstalacion` int(11) NOT NULL AUTO_INCREMENT,
  `tipo` varchar(100) NOT NULL,
  `capacidadMaxima` int(11) NOT NULL,
  `aforoActual` int(11) NOT NULL,
  PRIMARY KEY (`idInstalacion`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `pagos` (
  `idPago` int(11) NOT NULL AUTO_INCREMENT,
  `monto` decimal(15,4) DEFAULT NULL,
  `metodoPago` varchar(30) DEFAULT NULL,
  `estadoPago` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`idPago`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------
-- 2. TABLAS CON DEPENDENCIAS SIMPLES
-- --------------------------------------------------------

CREATE TABLE `usuarios` (
  `idusuario` int(11) NOT NULL AUTO_INCREMENT,
  `id_persona` int(11) NOT NULL,
  `contraseña` varchar(100) NOT NULL,
  `categoria` varchar(1) DEFAULT NULL,
  `esAfiliado` tinyint(1) NOT NULL,
  `id_rol` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`idusuario`),
  CONSTRAINT `fk_usuarios_persona` FOREIGN KEY (`id_persona`) REFERENCES `persona` (`id_persona`) ON DELETE CASCADE,
  CONSTRAINT `fk_usuarios_rol` FOREIGN KEY (`id_rol`) REFERENCES `rol` (`id_rol`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `entrenador` (
  `idEntrenador` int(11) NOT NULL AUTO_INCREMENT,
  `especialidad` varchar(50) NOT NULL,
  `numDocumento` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`idEntrenador`),
  CONSTRAINT `fk_entrenador_persona` FOREIGN KEY (`numDocumento`) REFERENCES `persona` (`numdocumento`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `administrador` (
  `id_administrador` int(11) NOT NULL,
  `contrasena_administrador` varchar(255) NOT NULL,
  PRIMARY KEY (`id_administrador`),
  CONSTRAINT `fk_admin_usuario` FOREIGN KEY (`id_administrador`) REFERENCES `usuarios` (`idusuario`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------
-- 3. ESPECIALIZACIONES DE INSTALACIÓN
-- --------------------------------------------------------

CREATE TABLE `gimnasio` (
  `idInstalacion` int(11) NOT NULL,
  `aforo_actual` int(11) DEFAULT 0,
  PRIMARY KEY (`idInstalacion`),
  CONSTRAINT `fk_gimnasio_instalacion` FOREIGN KEY (`idInstalacion`) REFERENCES `instalacion` (`idInstalacion`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `piscina` (
  `idInstalacion` int(11) NOT NULL,
  `numeroCarriles` int(11) NOT NULL,
  `profundidad` decimal(4,2) DEFAULT NULL,
  PRIMARY KEY (`idInstalacion`),
  CONSTRAINT `fk_piscina_instalacion` FOREIGN KEY (`idInstalacion`) REFERENCES `instalacion` (`idInstalacion`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------
-- 4. TABLAS DE PROCESOS (Turnos e Historial)
-- --------------------------------------------------------

CREATE TABLE `turno` (
  `idTurno` int(11) NOT NULL AUTO_INCREMENT,
  `fechaHora` datetime NOT NULL,
  `duracionMinutos` int(11) NOT NULL,
  `id_usuario` int(11) NOT NULL,
  `id_instalacion` int(11) NOT NULL,
  `numero_carril_assigned` int(11) DEFAULT NULL,
  `estado` varchar(20) DEFAULT 'RESERVADO',
  PRIMARY KEY (`idTurno`),
  CONSTRAINT `fk_turno_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`idusuario`) ON DELETE CASCADE,
  CONSTRAINT `fk_turno_instalacion` FOREIGN KEY (`id_instalacion`) REFERENCES `instalacion` (`idInstalacion`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `historial_citas` (
  `idHistorial` int(11) NOT NULL AUTO_INCREMENT,
  `id_turno` int(11) NOT NULL,
  `id_usuario` int(11) NOT NULL,
  `id_instalacion` int(11) NOT NULL,
  `estado` varchar(50) DEFAULT NULL,
  `fecha_evento` datetime DEFAULT current_timestamp(),
  `detalle` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`idHistorial`),
  CONSTRAINT `fk_historial_turno` FOREIGN KEY (`id_turno`) REFERENCES `turno` (`idTurno`) ON DELETE CASCADE,
  CONSTRAINT `fk_historial_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`idusuario`) ON DELETE CASCADE,
  CONSTRAINT `fk_historial_instalacion` FOREIGN KEY (`id_instalacion`) REFERENCES `instalacion` (`idInstalacion`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

COMMIT;