# =====================================================
# Makefile - Reserva de Postos de Carregamento
#
#   make          -> compila tudo
#   make server   -> arranca o servidor
#   make geral    -> arranca o cliente geral (RMI)
#   make admin    -> arranca o cliente admin (sockets)
#   make teste    -> testa a ligacao a BD
#   make clean    -> apaga os ficheiros compilados
# =====================================================

# Driver JDBC (unica biblioteca externa)
JDBC = lib/postgresql-42.7.14.jar

# Ficheiro de configuracao
CONFIG = config.properties

# Todos os ficheiros .java das 3 pastas
SRC = $(wildcard src/common/*.java src/server/*.java src/client/*.java)

# Classpath = pasta bin + driver JDBC
# No Windows separa-se com ";" e no Linux com ":"
# (o comando para apagar pastas tambem e diferente)
ifeq ($(OS),Windows_NT)
    CP    = bin;$(JDBC)
    APAGAR = rmdir /s /q bin
else
    CP    = bin:$(JDBC)
    APAGAR = rm -rf bin
endif

all:
	javac -encoding UTF-8 -d bin -cp "$(JDBC)" $(SRC)

server: all
	java -cp "$(CP)" server.Servidor $(CONFIG)

geral: all
	java -cp "$(CP)" client.ClienteGeral $(CONFIG)

admin: all
	java -cp "$(CP)" client.ClienteAdmin $(CONFIG)

teste: all
	java -cp "$(CP)" server.TesteLigacao $(CONFIG)

clean:
	$(APAGAR)
