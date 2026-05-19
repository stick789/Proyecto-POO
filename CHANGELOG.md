# Feats, Fixes y Checks

Gracias a todos por sus contribuciones a este proyecto a lo largo de estos meses.

A continuación hago presentacion del paso a paso de como acceder a los dashboards e informacion importante de los cambios realizados

---

## 1. Tipos de Login
En estos nuevos commits se realizo un rework casi total del sistema de login, se agregaron dos metodos funcionales para iniciar sesión; uno por los datos de la DB y otro por los datos hardcodeados.

## 2. Ingreso con datos 

### Datos Hardcodeados
Dichos datos hardcodeados son los siguientes: 

    1. admin@demo.com / admin123
    2. usuario@demo.com / user123

Estos datos les ayudaran a entrar al sistema actual para revisiones, cabe mencionar que esto se hace por el momento debido a que no hay nada inscrito en la base de datos para hacerlo con la base datos.

### Datos con la DB
Existe otro tipo de datos, los cuales son los recien añadidos a la DB, estos son datos de login los cuales son verificados con los metodos nuevos para poder ingresar teniendo conexión directa con la DB.

Dichos datos de la DB son los siguientes: 

    1. admin@test.com / admin123
    2. usuario@test.com / user123

Estos datos son los que se usarian ya con la base de datos, los cambios se hicieron en Personas, Administrador, Usuario por si necesitan ver que datos son.

## 3. Información Importante
Cabe mencionar, que debido a que no hay datos que rellenen la base datos que tenemos, los datos presentes son los que ustedes visualizarian en la demo.
Esto es gracias a que los metodos nuevos de el mejorado LoginControl y los Dashboard controllers, si se usan los datos de la DB, revisan que hay ahi para mostrar y debido a como no hay nada usan los otros
como backup.

`En el caso de los pagos es lo mismo, en ambos demo y db no se colocaron datos asi que primero se necesita el sistema de pagos para que guarden registros`

---

## 4. A Futuro

Como ya tenemos esa parte seria agregar embellecimientos que hagan que dichos dashboards queden mas bonitos asi como intentar agregar el panel visual de los carriles de las piscinas. 
Ademasz tambien hay que conectar lo de los pagos a los dashboards para que guarden ahi el registro y los jasperreports.
