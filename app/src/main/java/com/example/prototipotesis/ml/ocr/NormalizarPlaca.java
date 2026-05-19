package com.example.prototipotesis.ml.ocr;

import java.util.List;

public class NormalizarPlaca {

    private static final String[] PALABRAS_RUIDO = {
            "BOL", "BOLIV", "BOLI", "BOLIV", "BOLIVIA", "OLIV", "LIVIA"
    };

    /**
     * de los candidatos que se tienen, selecciona la mejor placa
     */
    public static String extraerMejor(List<String> candidatos) {
        if (candidatos == null || candidatos.isEmpty()) return "";

        String mejor = "";
        int mejorScore = 0;

        for (String texto : candidatos) {
            if (texto == null) continue;
            String limpio = texto
                    .toUpperCase()
                    .replaceAll("[^A-Z0-9]", "");

            // ignorar ruido (país u OCR basura)
            if (esRuido(limpio)) continue;

            // normalización de caracteres confusos
            limpio = normalizar(limpio);

            int score = calcularScore(limpio);

            if (score > mejorScore) {
                mejorScore = score;
                mejor = limpio;
            }
        }

        return mejor;
    }

    /**
     * Normaliza caracteres típicos de OCR en placas
     */
    public static String normalizar(String textoOCR) {
        if (textoOCR == null) return "";
        // eliminamos espacios y caracteres raros
        String cadenaLimpia = textoOCR.toUpperCase().replaceAll("[^A-Z0-9]", "");

        StringBuilder resultado = new StringBuilder();
        int longitudCadena = cadenaLimpia.length();
        int primeraMitad = 2;
        if (longitudCadena == 7){
            primeraMitad = 3;
        }
        for (int i = 0 ; i < longitudCadena ; i++){
            char caracter = cadenaLimpia.charAt(i);

            // primeras posiciones son numeros
            if (i <= primeraMitad){
                resultado.append(convertirANumero(caracter));
            }
            // ultimas posiciones son letras
            else{
                resultado.append(convertirALetra(caracter));
            }

            // if (resultado.length() == 7) break;
        }
        return resultado.toString();
    }

    /**
     * Filtra texto que no corresponde a placas reales
     */
    private static boolean esRuido(String texto) {
        if (texto == null) return true;
        for (String ruido : PALABRAS_RUIDO) {
            if (texto.contains(ruido)) return true;
        }
        return texto.length() < 4;
    }

    /**
     * Puntaje para elegir la mejor placa (que resultado parece mas una placa)
     */
    private static int calcularScore(String texto) {
        if (texto == null) return 0;
        int score = 0;

        // longitud típica de placas
        if (texto.length() >= 6 && texto.length() <= 7) score += 3;

        if (cuentaLetras(texto) >= 2) score += 2;

        if (cuentaNumeros(texto) >= 2) score += 2;

        // mezcla de letras y números (placa real)
        if (texto.matches(".*[A-Z].*") && texto.matches(".*[0-9].*")) {
            score += 5;
        }

        return score;
    }

    // contamos cantidad de numeros en una cadena
    private static int cuentaNumeros(String texto) {
        int c = 0;
        for (int i = 0; i < texto.length(); i++) {
            if (Character.isDigit(texto.charAt(i))) {
                c++;
            }
        }
        return c;
    }

    private static int cuentaLetras(String texto) {
        int c = 0;
        for (int i = 0; i < texto.length(); i++) {
            if (Character.isLetter(texto.charAt(i))) {
                c++;
            }
        }
        return c;
    }

    // conversio de numeros a letras
    private static char convertirALetra(char c) {
        switch (c) {
            case '0': return 'O';
            case '1': return 'I';
            case '2': return 'Z';
            case '5': return 'S';
            case '6': return 'G';
            case '7': return 'T';
            case '8': return 'B';
            default:
                return (c >= 'A' && c <= 'Z') ? c : 'X';
        }
    }

    // conversion de letras a numeros
    private static char convertirANumero(char c) {
        switch (c) {
            case 'O': return '0';
            case 'I': return '1';
            case 'Z': return '2';
            case 'S': return '5';
            case 'G': return '6';
            case 'T': return '7';
            case 'B': return '8';
            default:
                return (c >= '0' && c <= '9') ? c : '0';
        }
    }
}