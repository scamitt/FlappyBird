import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;

public class FlappyBird extends JPanel implements ActionListener, KeyListener {
    int boardWidth = 360;
    int boardHeight = 640;

    //images
    Image backgroundImg;
    Image birdImg;
    Image topPipeImg;
    Image bottomPipeImg;
    Image ghostImg;

    //bird class
    int birdX = boardWidth/8;
    int birdY = boardWidth/2;
    int birdWidth = 34;
    int birdHeight = 24;

    class Bird {
        int x = birdX;
        int y = birdY;
        int width = birdWidth;
        int height = birdHeight;
        Image img;

        Bird(Image img) {
            this.img = img;
        }
    }

    //pipe class
    int pipeX = boardWidth;
    int pipeY = 0;
    int pipeWidth = 64;  //scaled by 1/6
    int pipeHeight = 512;
    
    class Pipe {
        int x = pipeX;
        int y = pipeY;
        int width = pipeWidth;
        int height = pipeHeight;
        Image img;
        boolean hasGhost = false;
        boolean passed = false;

        Pipe(Image img) {
            this.img = img;
        }
    }

    Ghost ghost;
    int ghostX, ghostY;
    int ghostWidth = 26;  
    int ghostHeight = 26;
    boolean ghostMode = false;
    long ghostModeStartTime = 0;
    Image ghostBirdImg;

    class Ghost{
    
        int x = ghostX;
        int y = ghostY;
        int width = ghostWidth;
        int height = ghostHeight;
        Image img;
        
        Ghost(Image img) {
            this.img = img;
        }

    }

    
    //game logic
    Bird bird;
    int velocityX = -4; //move pipes to the left speed (simulates bird moving right)
    int velocityY = 0; //move bird up/down speed.
    int gravity = 1;
    

    ArrayList<Pipe> pipes;
    Random random = new Random();

    

    Timer gameLoop;
    Timer placePipeTimer;
    boolean gameOver = false;
    double score = 0;

    FlappyBird() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        addKeyListener(this);

        //load images
        backgroundImg = new ImageIcon(getClass().getResource("./flappybirdbg.png")).getImage();
        birdImg = new ImageIcon(getClass().getResource("./flappybird.png")).getImage();
        topPipeImg = new ImageIcon(getClass().getResource("./toppipe.png")).getImage();
        bottomPipeImg = new ImageIcon(getClass().getResource("./bottompipe.png")).getImage();
        ghostImg = new ImageIcon(getClass().getResource("./marioGhost.png")).getImage();
        ghostBirdImg = new ImageIcon(getClass().getResource("./ghostFlappyBird.png")).getImage();

        //bird
        bird = new Bird(birdImg);
        pipes = new ArrayList<Pipe>();
        ghost = new Ghost(ghostImg);

        //place pipes timer
        placePipeTimer = new Timer(1500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              placePipes();
            }
        });
        placePipeTimer.start();
        
		//game timer
		gameLoop = new Timer(1000/60, this); //how long it takes to start timer, milliseconds gone between frames 
        gameLoop.start();
	}
    
    void placePipes() {
        //(0-1) * pipeHeight/2.
        // 0 -> -128 (pipeHeight/4)
        // 1 -> -128 - 256 (pipeHeight/4 - pipeHeight/2) = -3/4 pipeHeight
        int randomPipeY = (int) (pipeY - pipeHeight/4 - Math.random()*(pipeHeight/2));
        int openingSpace = boardHeight/4;
    
        Pipe topPipe = new Pipe(topPipeImg);
        topPipe.y = randomPipeY;
        pipes.add(topPipe);
    
        Pipe bottomPipe = new Pipe(bottomPipeImg);
        bottomPipe.y = topPipe.y  + pipeHeight + openingSpace;
        pipes.add(bottomPipe);

        if(Math.random() <= 0.1){
            bottomPipe.hasGhost = true;
            ghost.height = 26;
            ghost.width = 26;
        }

    }
    
    
    public void paintComponent(Graphics g) {
		super.paintComponent(g);
		draw(g);
	}

	public void draw(Graphics g) {
        //background
        g.drawImage(backgroundImg, 0, 0, this.boardWidth, this.boardHeight, null);

        
        
        //pipes
        for (int i = 0; i < pipes.size(); i++) {
            Pipe pipe = pipes.get(i);
            g.drawImage(pipe.img, pipe.x, pipe.y, pipe.width, pipe.height, null);
            
            if (pipe.hasGhost == true) { 
                
                ghost.x = pipe.x + pipeWidth / 2 - ghost.width / 2; // Position the ghost between pipes
                ghost.y = pipe.y - boardHeight / 8 - ghost.height / 2; // Centered in the gap
                g.drawImage(ghostImg, ghost.x, ghost.y, ghost.width, ghost.height, null);

            }
            
        }  

        //bird
        if(ghostMode){
            g.drawImage(ghostBirdImg, bird.x, bird.y, bird.width, bird.height,null);
        }else{
            g.drawImage(birdImg, bird.x, bird.y, bird.width, bird.height, null);
        }

        //score
        g.setColor(Color.white);

        g.setFont(new Font("Arial", Font.PLAIN, 32));
        if (gameOver) {
            g.drawString("Game Over: " + String.valueOf((int) score), 10, 35);
        }
        else {
            g.drawString(String.valueOf((int) score), 10, 35);
        }
        
         
	}

    public void move() {
        velocityY += gravity;
        bird.y += velocityY;
        bird.y = Math.max(bird.y, 0); 

        for (int i = 0; i < pipes.size(); i++) {
            Pipe pipe = pipes.get(i);
            pipe.x += velocityX;

            if (!pipe.passed && bird.x > pipe.x + pipe.width) {
                score += 0.5; //0.5 because there are 2 pipes
                pipe.passed = true;
            }

            if (collision(bird, pipe) && !ghostMode) {
                gameOver = true;
            }
            // Check for collisions with pipes if not in ghost mode
            if (collisionWithGhost(bird)) {
                ghostMode = true;
                ghostModeStartTime = System.currentTimeMillis();
                ghost.height = 0; // Remove ghost from screen after collection
                ghost.width = 0;

            }

            //Handle ghost mode timing
            if (ghostMode && System.currentTimeMillis() - ghostModeStartTime > 7000) { // 15 seconds
                ghostMode = false;
            }   

        }

        if (bird.y > boardHeight) {
            gameOver = true;
        }
    }

    boolean collision(Bird a, Pipe b) {
        return a.x < b.x + b.width &&   //a's top left corner doesn't reach b's top right corner
               a.x + a.width > b.x &&   //a's top right corner passes b's top left corner
               a.y < b.y + b.height &&  //a's top left corner doesn't reach b's bottom left corner
               a.y + a.height > b.y;    //a's bottom left corner passes b's top left corner
    }

    boolean collisionWithGhost(Bird bird) {
        return bird.x < ghost.x + ghost.width &&
               bird.x + bird.width > ghost.x &&
               bird.y < ghost.y + ghost.height &&
               bird.y + bird.height > ghost.y;
    }
    
    

    @Override
    public void actionPerformed(ActionEvent e) { //called every x milliseconds by gameLoop timer
        move();
        repaint();
        if (gameOver) {
            placePipeTimer.stop();
            gameLoop.stop();
        }
    }  

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            // System.out.println("JUMP!");
            velocityY = -9;

            if (gameOver) {
                //restart game by resetting conditions
                bird.y = birdY;
                velocityY = 0;
                pipes.clear();
                gameOver = false;
                score = 0;
                gameLoop.start();
                placePipeTimer.start();
            }
        }
    }

    //not needed
    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}
}