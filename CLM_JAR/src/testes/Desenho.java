package testes;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;

/**
 *
 * @author Tiago Rodrigues
 *
 * Teste gerando imagens
 *
 */
public class Desenho extends JFrame {

    BufferedImage backBuffer;      // ESSE É O NOSSO BUFFER
    int FPS = 60;                  // ESSA É A TAXA DE ATUALIZAÇÃO DA TELA
    int telaLargura = 400;         // LARGURA DA TELA
    int telaAltura = 400;          // ALTURA DA TELA
    int mouse_x = 0;               // COORDENADA X DO MOUSE
    int mouse_y = 0;               // COORDENADA Y DO MOUSE
    List mousePointList = new ArrayList();

    // TESTE
    String codigo = "6310011701303";

    // Código de barras - Tabela    
    //  Legenda
    //  Formato 1	Formato 2	Descrição
    //  W               B               Preto largo
    //  N               b               Preto fino
    //  w               W               Branco largo
    //  n               w               Branco fino
    String[] dig0 = "NnNwWnWnN".split("");
    String[] dig1 = "WnNwNnNnW".split("");
    String[] dig2 = "NnWwNnNnW".split("");
    String[] dig3_dir = "0111101".split("");
    String[] dig4 = "NnNwWnNnW".split("");
    String[] dig5 = "WnNwWnNnN".split("");
    String[] dig6_dir = "0101111".split("");
   
    // NO NOSSO METODO ATUALIZAR VAMOS CHAMAR OS METODOS
    // QUE SERÃO EXECUTADOS O TEMPO INTEIRO...
    public void atualizar() {
        
    }
    
    //NESSE MÉTODO VAMOS DESENHAR
    //FORMAS GEOMETRICAS, IMAGENS E TEXTOS NA TELA E ETC...
    public void desenharGraficos() {
        Graphics g = getGraphics();     //COM g IREMOS DESENHAR O QUE ESTÁ NO BUFFER NA TELA

        //AQUI ESTAMOS DESENHANDO O BUFFER NA TELA,
        //NAS COORDENADAS X:0 e Y:0
        g.drawImage(backBuffer, 0, 0, this);
    }

    //ESSE É O NOSSO MÉTODO INICIALIZAR
    //AQUI VAMOS INICIALIZAR ALGUMAS CONFIGURAÇÃO DO frame E OUTRAS CONFIGURAÇÕES
    public void inicializar() {
        setTitle("Titulo da janela!");  //SETANDO O TITULO DA JANELA
        setSize(telaLargura, telaAltura);//DEFINIDO AS DIMENSÕES DA JANELA
        setResizable(false);    //TIRANDO A PERMISSÃO DO USUÁRIO REDIMENSIONAR A JANELA
        setDefaultCloseOperation(EXIT_ON_CLOSE);    //QUANDO FECHARMOS O frame A APLICAÇÃO PARA DE EXECUTAR
        setLayout(null);    //COM ISSO PODEREMOS DEFINIAR COORDENADA E DIMESÕES DE ELMENTOS DE FORMULARIO NO NOSSO FRAME
        setVisible(true);   //MUDANDO A VISIBILIDADE DO frame PARA TRUE, ASSIM ELE APARECERÁ
        backBuffer = new BufferedImage(telaLargura, telaAltura, BufferedImage.TYPE_INT_RGB);//CRIANDO O NOSSO BUFFER DE IMAGEM
    }

    //AQUI É O NOSSO MÉTODO RUN()
    //NELE TEMOS O NOSSO GAME LOOP (UM LOOP INFINITO)
    public void run() {
        inicializar();          //AQUI CHAMAMOS O METODO INICIALIZAR SOMENTE UMA VEZ, POIS ELE ESTÁ FORA DO NOSSO LOOP
        while (true) {          //AQUI É O NOSSO LOOP INFINITO
            atualizar();        //CHAMAMOS O METODO ATUALIZAR O TEMPO INTEIRO
            desenharGraficos(); //ATUALIZAREMOS O GRÁFICO QUE APARECE NA TELA O TEMPO INTEIRO
            try {
                Thread.sleep(1000 / FPS); //TAXA DE ATUALIZAÇÃO NA TELA, FUNCIONA COMO UM DELAY
            } catch (Exception e) {
                System.out.println("Thread interrompida!");
            }
        }
    }

    //AQUI É O NOSSO MÉTODO PRINCIPAL
    public static void main(String[] args) {
        Desenho game = new Desenho();     //CRIAMOS UM OBJETO A PARTIR DESSA PROPRIA CLASSE
        game.run();          //CHAMAMOS O METODO RUN(), O MÉTODO RUN() EXECUTA O INICIALIZAR(), ATUALIZAR() E DESENHARGRAFICOS()
    }
}
