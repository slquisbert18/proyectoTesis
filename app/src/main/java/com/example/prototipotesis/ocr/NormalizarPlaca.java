package com.example.prototipotesis.ocr;

public class NormalizarPlaca {
    public static String normalizar(String textoOCR){
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

    private static char convertirALetra(char c){
        switch (c){
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

    private static char convertirANumero(char c){
        switch(c){
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
