SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Crear la base de datos si no existe
--
CREATE DATABASE IF NOT EXISTS `proyecto_poo`;
USE `proyecto_poo`;

-- --------------------------------------------------------

-- 1. TABLAS INDEPENDIENTES
-- --------------------------------------------------------

CREATE TABLE IF NOT EXISTS `persona` (
  `id_persona` int(11) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) NOT NULL,
  `email` varchar(100) NOT NULL,
  `tipodocumento` varchar(100) NOT NULL,
  `numdocumento` varchar(100) NOT NULL,
  PRIMARY KEY (`id_persona`),
  UNIQUE KEY `idx_numdocumento` (`numdocumento`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `rol` (
  `id_rol` int(11) NOT NULL AUTO_INCREMENT,
  `nombre_rol` varchar(50) NOT NULL,
  `descripcion` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id_rol`),
  UNIQUE KEY `nombre_rol` (`nombre_rol`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `instalacion` (
  `idInstalacion` int(11) NOT NULL AUTO_INCREMENT,
  `tipo` varchar(100) NOT NULL,
  `capacidadMaxima` int(11) NOT NULL,
  `aforoActual` int(11) NOT NULL,
  `nombre` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`idInstalacion`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `pagos` (
  `idPago` int(11) NOT NULL AUTO_INCREMENT,
  `monto` decimal(15,4) DEFAULT NULL,
  `metodoPago` varchar(30) DEFAULT NULL,
  `estadoPago` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`idPago`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 2. TABLAS CON DEPENDENCIAS PRIMARIAS
-- --------------------------------------------------------

CREATE TABLE IF NOT EXISTS `usuarios` (
  `idusuario` int(11) NOT NULL AUTO_INCREMENT,
  `id_persona` int(11) NOT NULL,
  `contrasena` varchar(255) NOT NULL,
  `categoria` varchar(1) DEFAULT NULL,
  `esAfiliado` tinyint(1) NOT NULL,
  `id_rol` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`idusuario`),
  KEY `fk_usuarios_persona` (`id_persona`),
  KEY `fk_usuarios_rol` (`id_rol`),
  CONSTRAINT `fk_usuarios_persona` FOREIGN KEY (`id_persona`) REFERENCES `persona` (`id_persona`) ON DELETE CASCADE,
  CONSTRAINT `fk_usuarios_rol` FOREIGN KEY (`id_rol`) REFERENCES `rol` (`id_rol`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 3. TABLAS ESPECIALIZADAS (Administrador, Entrenador, Subtipos de Instalación)
-- --------------------------------------------------------

CREATE TABLE IF NOT EXISTS `administrador` (
  `id_administrador` int(11) NOT NULL,
  `contrasena_administrador` varchar(255) NOT NULL,
  PRIMARY KEY (`id_administrador`),
  CONSTRAINT `fk_admin_usuario` FOREIGN KEY (`id_administrador`) REFERENCES `usuarios` (`idusuario`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `entrenador` (
  `idEntrenador` int(11) NOT NULL AUTO_INCREMENT,
  `especialidad` varchar(50) NOT NULL,
  `numDocumento` varchar(100) DEFAULT NULL,
  `idInstalacion` int(11) DEFAULT NULL,
  PRIMARY KEY (`idEntrenador`),
  KEY `fk_entrenador_persona` (`numDocumento`),
  KEY `fk_entrenador_instalacion` (`idInstalacion`),
  CONSTRAINT `fk_entrenador_instalacion` FOREIGN KEY (`idInstalacion`) REFERENCES `instalacion` (`idInstalacion`),
  CONSTRAINT `fk_entrenador_persona` FOREIGN KEY (`numDocumento`) REFERENCES `persona` (`numdocumento`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `gimnasio` (
  `idInstalacion` int(11) NOT NULL,
  `aforo_actual` int(11) DEFAULT 0,
  PRIMARY KEY (`idInstalacion`),
  CONSTRAINT `fk_gimnasio_instalacion` FOREIGN KEY (`idInstalacion`) REFERENCES `instalacion` (`idInstalacion`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `piscina` (
  `idInstalacion` int(11) NOT NULL,
  `numeroCarriles` int(11) NOT NULL,
  `profundidad` decimal(4,2) DEFAULT NULL,
  PRIMARY KEY (`idInstalacion`),
  CONSTRAINT `fk_piscina_instalacion` FOREIGN KEY (`idInstalacion`) REFERENCES `instalacion` (`idInstalacion`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 4. TABLAS DE OPERACIÓN (Turnos e Historial)
-- --------------------------------------------------------

CREATE TABLE IF NOT EXISTS `turno` (
  `idTurno` int(11) NOT NULL AUTO_INCREMENT,
  `fechaHora` datetime NOT NULL,
  `duracionMinutos` int(11) NOT NULL,
  `id_usuario` int(11) NOT NULL,
  `id_instalacion` int(11) NOT NULL,
  `id_entrenador` int(11) DEFAULT NULL,
  `numero_carril_assigned` int(11) DEFAULT NULL,
  `estado` varchar(20) DEFAULT 'RESERVADO',
  PRIMARY KEY (`idTurno`),
  KEY `fk_turno_usuario` (`id_usuario`),
  KEY `fk_turno_instalacion` (`id_instalacion`),
  CONSTRAINT `fk_turno_instalacion` FOREIGN KEY (`id_instalacion`) REFERENCES `instalacion` (`idInstalacion`) ON DELETE CASCADE,
  CONSTRAINT `fk_turno_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`idusuario`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `historial_citas` (
  `idHistorial` int(11) NOT NULL AUTO_INCREMENT,
  `id_turno` int(11) NOT NULL,
  `id_usuario` int(11) NOT NULL,
  `id_instalacion` int(11) NOT NULL,
  `estado` varchar(50) DEFAULT NULL,
  `fecha_evento` datetime DEFAULT current_timestamp(),
  `detalle` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`idHistorial`),
  KEY `fk_historial_turno` (`id_turno`),
  KEY `fk_historial_usuario` (`id_usuario`),
  KEY `fk_historial_instalacion` (`id_instalacion`),
  CONSTRAINT `fk_historial_instalacion` FOREIGN KEY (`id_instalacion`) REFERENCES `instalacion` (`idInstalacion`) ON DELETE CASCADE,
  CONSTRAINT `fk_historial_turno` FOREIGN KEY (`id_turno`) REFERENCES `turno` (`idTurno`) ON DELETE CASCADE,
  CONSTRAINT `fk_historial_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`idusuario`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 5. INSERCIÓN DE DATOS MAESTROS
-- --------------------------------------------------------

INSERT IGNORE INTO `rol` (`id_rol`, `nombre_rol`, `descripcion`) VALUES
(1, 'USUARIO', 'Usuario regular del sistema'),
(2, 'ADMINISTRADOR', 'Administrador del sistema'),
(3, 'ENTRENADOR', 'Entrenador del gimnasio o piscina');

COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
