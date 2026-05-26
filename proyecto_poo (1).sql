-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Servidor: 127.0.0.1
-- Tiempo de generación: 26-05-2026 a las 03:28:02
-- Versión del servidor: 10.4.32-MariaDB
-- Versión de PHP: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de datos: `proyecto_poo`
--

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `administrador`
--

CREATE TABLE `administrador` (
  `id_administrador` int(11) NOT NULL,
  `contraseña_administrador` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `administrador`
--

INSERT INTO `administrador` (`id_administrador`, `contraseña_administrador`) VALUES
(1, '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `entrenador`
--

CREATE TABLE `entrenador` (
  `idEntrenador` int(11) NOT NULL,
  `especialidad` varchar(50) NOT NULL,
  `numDocumento` varchar(100) DEFAULT NULL,
  `idInstalacion` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `gimnasio`
--

CREATE TABLE `gimnasio` (
  `idInstalacion` int(11) NOT NULL,
  `aforo_actual` int(11) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `gimnasio`
--

INSERT INTO `gimnasio` (`idInstalacion`, `aforo_actual`) VALUES
(2, 30);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `historial_citas`
--

CREATE TABLE `historial_citas` (
  `idHistorial` int(11) NOT NULL,
  `id_turno` int(11) NOT NULL,
  `id_usuario` int(11) NOT NULL,
  `id_instalacion` int(11) NOT NULL,
  `estado` varchar(50) DEFAULT NULL,
  `fecha_evento` datetime DEFAULT current_timestamp(),
  `detalle` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `historial_citas`
--

INSERT INTO `historial_citas` (`idHistorial`, `id_turno`, `id_usuario`, `id_instalacion`, `estado`, `fecha_evento`, `detalle`) VALUES
(1, 2, 2, 3, 'RESERVADO', '2026-05-25 20:18:39', 'Turno reservado.'),
(2, 3, 2, 3, 'RESERVADO', '2026-05-25 20:19:59', 'Turno reservado.'),
(3, 4, 2, 3, 'RESERVADO', '2026-05-25 20:25:59', 'Turno reservado.');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `instalacion`
--

CREATE TABLE `instalacion` (
  `idInstalacion` int(11) NOT NULL,
  `tipo` varchar(100) NOT NULL,
  `capacidadMaxima` int(11) NOT NULL,
  `aforoActual` int(11) NOT NULL,
  `nombre` varchar(100) DEFAULT NULL,
  `idSede` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `instalacion`
--

INSERT INTO `instalacion` (`idInstalacion`, `tipo`, `capacidadMaxima`, `aforoActual`, `nombre`, `idSede`) VALUES
(2, 'PISCINA', 20, 20, 'Piscina CUR', 1),
(3, 'GIMNASIO', 30, 27, 'Gimnasio CUR', 1),
(5, 'Piscina', 30, 0, 'Piscina Olímpica CUR', 2);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `pagos`
--

CREATE TABLE `pagos` (
  `idPago` int(11) NOT NULL,
  `monto` decimal(15,4) DEFAULT NULL,
  `metodoPago` varchar(30) DEFAULT NULL,
  `estadoPago` varchar(30) DEFAULT NULL,
  `id_turno` int(11) DEFAULT NULL,
  `id_usuario` int(11) DEFAULT NULL,
  `fechaPago` datetime DEFAULT current_timestamp(),
  `epayco_session_id` varchar(120) DEFAULT NULL,
  `epayco_ref_payco` varchar(120) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `pagos`
--

INSERT INTO `pagos` (`idPago`, `monto`, `metodoPago`, `estadoPago`, `id_turno`, `id_usuario`, `fechaPago`, `epayco_session_id`, `epayco_ref_payco`) VALUES
(1, 50000.0000, 'TARJETA', 'PAGADO', NULL, NULL, '2026-05-25 20:24:26', NULL, NULL),
(2, 15000.0000, 'TARJETA_ONLINE', 'PENDIENTE', 4, 2, '2026-05-25 20:26:00', '6a14f6a860f857ef9a5b3360', NULL);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `persona`
--

CREATE TABLE `persona` (
  `id_persona` int(11) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `email` varchar(100) NOT NULL,
  `tipodocumento` varchar(100) NOT NULL,
  `numdocumento` varchar(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `persona`
--

INSERT INTO `persona` (`id_persona`, `nombre`, `email`, `tipodocumento`, `numdocumento`) VALUES
(1, 'Admin Principal', 'admin@test.com', 'CC', '11111111'),
(2, 'Usuario Prueba', 'usuario@test.com', 'CC', '22222222'),
(3, 'Laura Pineda', 'laura.pineda@test.com', 'CC', '33333333'),
(4, 'Andres Rojas', 'andres.rojas@test.com', 'CC', '44444444'),
(5, 'Sofia Gomez', 'sofia.gomez@test.com', 'CC', '55555555'),
(6, 'Carlos Mejia', 'carlos.mejia@test.com', 'CC', '66666666');

-- --------------------------------------------------------

--
-- Volcado de datos para la tabla `entrenador`
--

INSERT INTO `entrenador` (`idEntrenador`, `especialidad`, `numDocumento`, `idInstalacion`) VALUES
(1, 'Natación', '33333333', 2),
(2, 'Natación', '44444444', 2),
(3, 'Gimnasio', '55555555', 3),
(4, 'Gimnasio', '66666666', 3);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `piscina`
--

CREATE TABLE `piscina` (
  `idInstalacion` int(11) NOT NULL,
  `numeroCarriles` int(11) NOT NULL,
  `profundidad` decimal(4,2) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `piscina`
--

INSERT INTO `piscina` (`idInstalacion`, `numeroCarriles`, `profundidad`) VALUES
(2, 8, 2.00);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `rol`
--

CREATE TABLE `rol` (
  `id_rol` int(11) NOT NULL,
  `nombre_rol` varchar(50) NOT NULL,
  `descripcion` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `rol`
--

INSERT INTO `rol` (`id_rol`, `nombre_rol`, `descripcion`) VALUES
(1, 'USUARIO', 'Usuario regular del sistema'),
(2, 'ADMINISTRADOR', 'Administrador del sistema'),
(3, 'ENTRENADOR', 'Entrenador del gimnasio o piscina');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `sede`
--

CREATE TABLE `sede` (
  `idSede` int(11) NOT NULL,
  `nombre` varchar(150) NOT NULL,
  `direccion` varchar(255) DEFAULT NULL,
  `telefono` varchar(50) DEFAULT NULL,
  `email` varchar(150) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `sede`
--

INSERT INTO `sede` (`idSede`, `nombre`, `direccion`, `telefono`, `email`) VALUES
(1, 'CUR', 'Cra 69 #49a-73, Bogotá', '(601) 3077001', 'pqrs@compensar.com'),
(2, 'Sede Norte CUR', NULL, NULL, NULL);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `turno`
--

CREATE TABLE `turno` (
  `idTurno` int(11) NOT NULL,
  `fechaHora` datetime NOT NULL,
  `duracionMinutos` int(11) NOT NULL,
  `id_usuario` int(11) NOT NULL,
  `id_instalacion` int(11) NOT NULL,
  `id_entrenador` int(11) DEFAULT NULL,
  `estado` varchar(20) DEFAULT 'RESERVADO',
  `idSede` int(11) DEFAULT NULL,
  `numero_carril_asignado` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `turno`
--

INSERT INTO `turno` (`idTurno`, `fechaHora`, `duracionMinutos`, `id_usuario`, `id_instalacion`, `id_entrenador`, `estado`, `idSede`, `numero_carril_asignado`) VALUES
(1, '2026-05-25 08:00:00', 60, 2, 2, NULL, 'RESERVADO', 1, NULL),
(2, '2026-05-26 08:00:00', 60, 2, 3, NULL, 'RESERVADO', NULL, NULL),
(3, '2026-05-26 09:30:00', 60, 2, 3, NULL, 'RESERVADO', NULL, NULL),
(4, '2026-05-26 06:00:00', 60, 2, 3, NULL, 'RESERVADO', NULL, NULL);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `usuarios`
--

CREATE TABLE `usuarios` (
  `idusuario` int(11) NOT NULL,
  `id_persona` int(11) NOT NULL,
  `contraseña` varchar(255) NOT NULL,
  `categoria` varchar(20) DEFAULT NULL,
  `esAfiliado` tinyint(1) NOT NULL,
  `id_rol` int(11) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `usuarios`
--

INSERT INTO `usuarios` (`idusuario`, `id_persona`, `contraseña`, `categoria`, `esAfiliado`, `id_rol`) VALUES
(1, 1, '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', NULL, 0, 2),
(2, 2, 'e606e38b0d8c19b24cf0ee3808183162ea7cd63ff7912dbb22b5e803286b4446', 'A', 1, 1),
(3, 3, 'e606e38b0d8c19b24cf0ee3808183162ea7cd63ff7912dbb22b5e803286b4446', NULL, 0, 3),
(4, 4, 'e606e38b0d8c19b24cf0ee3808183162ea7cd63ff7912dbb22b5e803286b4446', NULL, 0, 3),
(5, 5, 'e606e38b0d8c19b24cf0ee3808183162ea7cd63ff7912dbb22b5e803286b4446', NULL, 0, 3),
(6, 6, 'e606e38b0d8c19b24cf0ee3808183162ea7cd63ff7912dbb22b5e803286b4446', NULL, 0, 3);

--
-- Índices para tablas volcadas
--

--
-- Indices de la tabla `administrador`
--
ALTER TABLE `administrador`
  ADD PRIMARY KEY (`id_administrador`);

--
-- Indices de la tabla `entrenador`
--
ALTER TABLE `entrenador`
  ADD PRIMARY KEY (`idEntrenador`),
  ADD KEY `fk_entrenador_persona` (`numDocumento`),
  ADD KEY `fk_entrenador_instalacion` (`idInstalacion`);

--
-- Indices de la tabla `gimnasio`
--
ALTER TABLE `gimnasio`
  ADD PRIMARY KEY (`idInstalacion`);

--
-- Indices de la tabla `historial_citas`
--
ALTER TABLE `historial_citas`
  ADD PRIMARY KEY (`idHistorial`),
  ADD KEY `fk_historial_turno` (`id_turno`),
  ADD KEY `fk_historial_usuario` (`id_usuario`),
  ADD KEY `fk_historial_instalacion` (`id_instalacion`);

--
-- Indices de la tabla `instalacion`
--
ALTER TABLE `instalacion`
  ADD PRIMARY KEY (`idInstalacion`),
  ADD KEY `fk_instalacion_sede` (`idSede`);

--
-- Indices de la tabla `pagos`
--
ALTER TABLE `pagos`
  ADD PRIMARY KEY (`idPago`);

--
-- Indices de la tabla `persona`
--
ALTER TABLE `persona`
  ADD PRIMARY KEY (`id_persona`),
  ADD UNIQUE KEY `idx_numdocumento` (`numdocumento`);

--
-- Indices de la tabla `piscina`
--
ALTER TABLE `piscina`
  ADD PRIMARY KEY (`idInstalacion`);

--
-- Indices de la tabla `rol`
--
ALTER TABLE `rol`
  ADD PRIMARY KEY (`id_rol`),
  ADD UNIQUE KEY `nombre_rol` (`nombre_rol`);

--
-- Indices de la tabla `sede`
--
ALTER TABLE `sede`
  ADD PRIMARY KEY (`idSede`),
  ADD UNIQUE KEY `uniq_sede_nombre` (`nombre`);

--
-- Indices de la tabla `turno`
--
ALTER TABLE `turno`
  ADD PRIMARY KEY (`idTurno`),
  ADD KEY `fk_turno_usuario` (`id_usuario`),
  ADD KEY `fk_turno_instalacion` (`id_instalacion`),
  ADD KEY `fk_turno_sede` (`idSede`);

--
-- Indices de la tabla `usuarios`
--
ALTER TABLE `usuarios`
  ADD PRIMARY KEY (`idusuario`),
  ADD KEY `fk_usuarios_persona` (`id_persona`),
  ADD KEY `fk_usuarios_rol` (`id_rol`);

--
-- AUTO_INCREMENT de las tablas volcadas
--

--
-- AUTO_INCREMENT de la tabla `entrenador`
--
-- --------------------------------------------------------
-- AÑADIDO: columna `activo` en `usuarios` para control de activación
-- Si la tabla ya existe sin la columna, este ALTER TABLE la creará.
ALTER TABLE `usuarios`
  ADD COLUMN `activo` TINYINT(1) NOT NULL DEFAULT 1;

ALTER TABLE `entrenador`
  MODIFY `idEntrenador` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=14;

-- --------------------------------------------------------
-- AÑADIDO: columnas para registrar cancelaciones de turnos
-- Si la tabla `turno` existe sin estas columnas, este ALTER las creará.
ALTER TABLE `turno`
  ADD COLUMN `cancelado_por` int(11) DEFAULT NULL,
  ADD COLUMN `cancelado_en` datetime DEFAULT NULL;


--
-- AUTO_INCREMENT de la tabla `historial_citas`
--
ALTER TABLE `historial_citas`
  MODIFY `idHistorial` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT de la tabla `instalacion`
--
ALTER TABLE `instalacion`
  MODIFY `idInstalacion` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT de la tabla `pagos`
--
ALTER TABLE `pagos`
  MODIFY `idPago` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT de la tabla `persona`
--
ALTER TABLE `persona`
  MODIFY `id_persona` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=14;

--
-- AUTO_INCREMENT de la tabla `rol`
--
ALTER TABLE `rol`
  MODIFY `id_rol` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT de la tabla `sede`
--
ALTER TABLE `sede`
  MODIFY `idSede` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT de la tabla `turno`
--
ALTER TABLE `turno`
  MODIFY `idTurno` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT de la tabla `usuarios`
--
ALTER TABLE `usuarios`
  MODIFY `idusuario` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=14;

--
-- Restricciones para tablas volcadas
--

--
-- Filtros para la tabla `administrador`
--
ALTER TABLE `administrador`
  ADD CONSTRAINT `fk_admin_usuario` FOREIGN KEY (`id_administrador`) REFERENCES `usuarios` (`idusuario`) ON DELETE CASCADE;

--
-- Filtros para la tabla `entrenador`
--
ALTER TABLE `entrenador`
  ADD CONSTRAINT `fk_entrenador_instalacion` FOREIGN KEY (`idInstalacion`) REFERENCES `instalacion` (`idInstalacion`),
  ADD CONSTRAINT `fk_entrenador_persona` FOREIGN KEY (`numDocumento`) REFERENCES `persona` (`numdocumento`) ON DELETE CASCADE;

--
-- Filtros para la tabla `gimnasio`
--
ALTER TABLE `gimnasio`
  ADD CONSTRAINT `fk_gimnasio_instalacion` FOREIGN KEY (`idInstalacion`) REFERENCES `instalacion` (`idInstalacion`) ON DELETE CASCADE;

--
-- Filtros para la tabla `historial_citas`
--
ALTER TABLE `historial_citas`
  ADD CONSTRAINT `fk_historial_instalacion` FOREIGN KEY (`id_instalacion`) REFERENCES `instalacion` (`idInstalacion`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_historial_turno` FOREIGN KEY (`id_turno`) REFERENCES `turno` (`idTurno`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_historial_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`idusuario`) ON DELETE CASCADE;

--
-- Filtros para la tabla `instalacion`
--
ALTER TABLE `instalacion`
  ADD CONSTRAINT `fk_instalacion_sede` FOREIGN KEY (`idSede`) REFERENCES `sede` (`idSede`) ON DELETE SET NULL;

--
-- Filtros para la tabla `piscina`
--
ALTER TABLE `piscina`
  ADD CONSTRAINT `fk_piscina_instalacion` FOREIGN KEY (`idInstalacion`) REFERENCES `instalacion` (`idInstalacion`) ON DELETE CASCADE;

--
-- Filtros para la tabla `turno`
--
ALTER TABLE `turno`
  ADD CONSTRAINT `fk_turno_instalacion` FOREIGN KEY (`id_instalacion`) REFERENCES `instalacion` (`idInstalacion`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_turno_sede` FOREIGN KEY (`idSede`) REFERENCES `sede` (`idSede`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_turno_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`idusuario`) ON DELETE CASCADE;

--
-- Filtros para la tabla `usuarios`
--
ALTER TABLE `usuarios`
  ADD CONSTRAINT `fk_usuarios_persona` FOREIGN KEY (`id_persona`) REFERENCES `persona` (`id_persona`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_usuarios_rol` FOREIGN KEY (`id_rol`) REFERENCES `rol` (`id_rol`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
