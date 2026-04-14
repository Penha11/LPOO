import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import javax.swing.*;

// --- ESTRUTURA LPOO ---
abstract class ElementoMarinho {
    protected double x, y;
    protected int largura, altura;
    protected Image imagem;

    public ElementoMarinho(double x, double y, int w, int h, String imgPath) {
        this.x = x; this.y = y; this.largura = w; this.altura = h;
        try {
            this.imagem = new ImageIcon(getClass().getResource(imgPath)).getImage();
        } catch (Exception e) {
            // Caso a imagem não exista, o jogo não trava
        }
    }
    public void desenhar(Graphics2D g2d) { 
        if(imagem != null) g2d.drawImage(imagem, (int)x, (int)y, largura, altura, null);
        else { g2d.setColor(Color.MAGENTA); g2d.fillRect((int)x, (int)y, largura, altura); } // Placeholder
    }
    public Rectangle getBounds() { return new Rectangle((int)x, (int)y, largura, altura); }
}

class Lixo extends ElementoMarinho {
    private double vel;
    public Lixo(double x, double y, double vel) { super(x, y, 35, 35, "lixo.png"); this.vel = vel; }
    public void cair() { y += vel; }
}

class AnimalMarinho extends ElementoMarinho {
    private double vel;
    public AnimalMarinho(double x, double y, double v) { super(x, y, 65, 45, "peixe.png"); this.vel = v + new Random().nextDouble() * 2; }
    public void mover() { x += vel; if (x > 850) x = -70; }
}

public class JogoOceanGuard extends JPanel implements ActionListener {
    private enum Estado { MENU, JOGANDO, PAUSE, GAME_OVER, VITORIA }
    private Estado estadoAtual = Estado.MENU;

    private int tempoRestante, vidas, pontos;
    private double velLixo, velAnimal, playerX, playerY;
    private boolean atacando = false;
    private int timerAtaque = 0;

    private final ArrayList<Lixo> lixos = new ArrayList<>();
    private final ArrayList<AnimalMarinho> animais = new ArrayList<>();
    private Timer gameLoop, clockTimer;
    private final Random rnd = new Random();

    // Assets
    private Image imgCenario = new ImageIcon(getClass().getResource("cenario.png")).getImage();
    private Image imgSub = new ImageIcon(getClass().getResource("submarino.png")).getImage();
    private Image imgRede = new ImageIcon(getClass().getResource("rede.png")).getImage();

    public JogoOceanGuard() {
        setFocusable(true);
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (estadoAtual == Estado.JOGANDO) { playerX = e.getX()-30; playerY = e.getY()-20; }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (estadoAtual == Estado.MENU) checarCliqueMenu(e.getPoint());
                else if (estadoAtual == Estado.PAUSE) checarCliquePause(e.getPoint());
                else if (estadoAtual == Estado.JOGANDO) { atacando = true; timerAtaque = 12; }
                else estadoAtual = Estado.MENU;
            }
        });

        // ESC para Pause
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "pause");
        getActionMap().put("pause", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if(estadoAtual == Estado.JOGANDO) estadoAtual = Estado.PAUSE;
                else if(estadoAtual == Estado.PAUSE) estadoAtual = Estado.JOGANDO;
            }
        });

        gameLoop = new Timer(16, this);
        gameLoop.start();
    }

    private void configurarDificuldade(String modo) {
        animais.clear(); lixos.clear(); pontos = 0;
        if(modo.equals("FACIL")) { tempoRestante = 15; velLixo = 2.5; velAnimal = 1.5; vidas = 3; }
        else if(modo.equals("NORMAL")) { tempoRestante = 30; velLixo = 4.0; velAnimal = 3.0; vidas = 3; }
        else { tempoRestante = 60; velLixo = 6.0; velAnimal = 5.0; vidas = 3; }

        for(int i=0; i<5; i++) animais.add(new AnimalMarinho(rnd.nextInt(700), 120 + i*85, velAnimal));
        if(clockTimer != null) clockTimer.stop();
        clockTimer = new Timer(1000, e -> {
            if(estadoAtual == Estado.JOGANDO && tempoRestante > 0) tempoRestante--;
            else if(tempoRestante <= 0) { estadoAtual = Estado.VITORIA; clockTimer.stop(); }
        });
        clockTimer.start();
        estadoAtual = Estado.JOGANDO;
    }

    private void checarCliqueMenu(Point p) {
        if(new Rectangle(300, 200, 200, 50).contains(p)) configurarDificuldade("FACIL");
        if(new Rectangle(300, 270, 200, 50).contains(p)) configurarDificuldade("NORMAL");
        if(new Rectangle(300, 340, 200, 50).contains(p)) configurarDificuldade("DIFICIL");
    }

    private void checarCliquePause(Point p) {
        if(new Rectangle(300, 250, 200, 50).contains(p)) estadoAtual = Estado.JOGANDO;
        if(new Rectangle(300, 320, 200, 50).contains(p)) estadoAtual = Estado.MENU;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (estadoAtual == Estado.JOGANDO) {
            if (rnd.nextInt(100) < 7) lixos.add(new Lixo(rnd.nextInt(750), -30, velLixo));
            Iterator<Lixo> it = lixos.iterator();
            while (it.hasNext()) {
                Lixo l = it.next(); l.cair();
                Rectangle rRede = new Rectangle((int)playerX-35, (int)playerY-35, 130, 110);
                if (atacando && rRede.intersects(l.getBounds())) { it.remove(); pontos += 10; continue; }
                for (AnimalMarinho a : animais) if (a.getBounds().intersects(l.getBounds())) { it.remove(); vidas--; break; }
                if (l.y > 600) it.remove();
            }
            if (vidas <= 0) { estadoAtual = Estado.GAME_OVER; clockTimer.stop(); }
            for (AnimalMarinho a : animais) a.mover();
            if (atacando && --timerAtaque <= 0) atacando = false;
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(imgCenario, 0, 0, 800, 600, null);

        if (estadoAtual == Estado.MENU) desenharMenu(g2d);
        else if (estadoAtual == Estado.PAUSE) desenharPause(g2d);
        else desenharJogo(g2d);
    }

    private void desenharMenu(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fillRect(0, 0, 800, 600);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 40));
        g2d.drawString("OCEAN GUARD", 250, 100);

        String[] t = {"FÁCIL", "NORMAL", "DIFÍCIL"};
        for(int i=0; i<3; i++) {
            g2d.setColor(Color.WHITE);
            g2d.fillRoundRect(300, 200 + (i*70), 200, 50, 15, 15);
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 18));
            g2d.drawString(t[i], 365, 232 + (i*70));
        }
    }

    private void desenharPause(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, 800, 600);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 30));
        g2d.drawString("PAUSADO", 330, 180);
        
        String[] b = {"CONTINUAR", "MENU PRINCIPAL"};
        for(int i=0; i<2; i++) {
            g2d.setColor(Color.WHITE);
            g2d.fillRoundRect(300, 250 + (i*70), 200, 50, 15, 15);
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString(b[i], 320 + (i==0?25:10), 282 + (i*70));
        }
    }

    private void desenharJogo(Graphics2D g2d) {
        for (AnimalMarinho a : animais) a.desenhar(g2d);
        for (Lixo l : lixos) l.desenhar(g2d);
        g2d.drawImage(imgSub, (int)playerX, (int)playerY, 70, 50, null);
        if (atacando) g2d.drawImage(imgRede, (int)playerX-30, (int)playerY-25, 130, 100, null);

        // PLACAR EM PRETO PARA VISIBILIDADE
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("TEMPO: " + tempoRestante + "s | VIDAS: " + vidas + " | PONTOS: " + pontos, 20, 35);
        
        if (estadoAtual == Estado.GAME_OVER) {
            g2d.setFont(new Font("Arial", Font.BOLD, 30));
            g2d.drawString("FIM DE JOGO!", 300, 300);
        }
        if (estadoAtual == Estado.VITORIA) {
            g2d.setFont(new Font("Arial", Font.BOLD, 30));
            g2d.drawString("VOCÊ SALVOU O MAR!", 240, 300);
        }
    }

    public static void main(String[] args) {
        JFrame f = new JFrame("APS UNIP - Ocean Guard");
        f.add(new JogoOceanGuard());
        f.setSize(800, 600);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}