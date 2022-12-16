package application;

import java.util.List;
import java.util.Random;

import javafx.application.Application;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class FishSlayer extends Application{
//	variables
	private static final Random RAND = new Random();
	private static final int WIDTH = 800;
	private static final int HEIGHT = 600;
	private static final int PLAYER_SIZE = 60;
	
	static final int EXPLOSION_W = 128;
	static final int EXPLOSION_H = 128;
	static final int EXPLOSION_ROWS = 3;
	static final int EXPLOSION_COL = 3;
	static final int EXPLOSION_STEPS = 15;
	
	static final Image PLAYER_IMG = new Image("file:src/application/img/player.png");
	static final Image CAUGHT_IMG = new Image("file:src/application/img/caught.png");
	
	static final Image FISHES_IMG[] = {
		new Image("file:src/application/img/01.png"),
		new Image("file:src/application/img/02.png"),
		new Image("file:src/application/img/03.png"),
		new Image("file:src/application/img/04.png"),
		new Image("file:src/application/img/05.png"),
		new Image("file:src/application/img/06.png"),
		new Image("file:src/application/img/07.png"),
		new Image("file:src/application/img/08.png"),
		new Image("file:src/application/img/09.png"),
		new Image("file:src/application/img/10.png"),
		new Image("file:src/application/img/11.png"),
		new Image("file:src/application/img/12.png")
	};
	
	final int MAX_FISHES = 6;
	final int MAX_SHOTS = MAX_FISHES;
	boolean gameOver = false;
	private GraphicsContext gc;
	
	Ship player;
	List<Net> nets;
	List<Ocean> oceans;
	List<Fish> fishes;
	
	private double mouseX;
	private int score;
	
	//run graphics
  	private void run(GraphicsContext gc) {
  		gc.setFill(Color.grayRgb(20));
  		gc.fillRect(0, 0, WIDTH, HEIGHT);
  		gc.setTextAlign(TextAlignment.CENTER);
  		gc.setFont(Font.font(20));
  		gc.setFill(Color.WHITE);
  		gc.fillText("Score: " + score, 60, 20);

  		if(gameOver) {
  			gc.setFont(Font.font(35));
  			gc.setFill(Color.YELLOW);
  			gc.fillText("GameOver \n Your Score is: " + score + "\nClick to play again", WIDTH/2, HEIGHT/2.5);
  		}
  		
  		oceans.forEach(Ocean::draw);
		
  		player.update();
		player.draw();
		player.posX= (int) mouseX;

		fishes.stream().peek(Ship::update).peek(Ship::draw).forEach(e ->{
			if(player.collide(e) && !player.exploding) {
				player.explode();
			}
		});
			
		for(int i = nets.size() - 1; i >= 0 ; i--) {
			Net net = nets.get(i);
			if(net.posY <0 || net.toRemove) {
				nets.remove(i);
				continue;
			}
			net.update();
			net.draw();
			for(Fish fish : fishes) {
				if(net.collide(fish) && !fish.exploding) {
					score++;
					fish.explode();
					net.toRemove= true;
				}
			}
		}
			
		for(int i = fishes.size() - 1; i>=0; i--) {
			if(fishes.get(i).destroyed) {
				fishes.set(i,newBomb());
			}
		}
		
		gameOver = player.destroyed;
		
		if(RAND.nextInt(10)>2) {
			oceans.add(new Ocean());
		}
		
		for(int i = 0; i< oceans.size(); i++) {
			if(oceans.get(i).posY > HEIGHT)
				oceans.remove(i);
		}
	}
	//start
	public void start(Stage stage) throws Exception {
		Canvas canvas = new Canvas(WIDTH, HEIGHT);	
		gc = canvas.getGraphicsContext2D();

		Timeline timeline = new Timeline(new KeyFrame(Duration.millis(100), e -> run(gc)));
		timeline.setCycleCount(Timeline.INDEFINITE);
		timeline.play();

		canvas.setCursor(Cursor.MOVE);
		canvas.setOnMouseMoved(e -> mouseX = e.getX());

		canvas.setOnMouseClicked(e -> {
			if(nets.size() < MAX_SHOTS) 
				nets.add(player.shoot());
			
			if(gameOver) { 
				gameOver = false;
				setup();
			}
		});

		setup();
			
		stage.setScene(new Scene(new StackPane(canvas)));
		stage.setTitle("Fish Slayer");
		stage.show();
	}
	
	private void setup() {
		oceans = new ArrayList<>();
		nets = new ArrayList<>();
		fishes = new ArrayList<>();
		player = new Ship(WIDTH / 2, HEIGHT - PLAYER_SIZE, PLAYER_SIZE, PLAYER_IMG);
		score = 0;
		IntStream.range(0, MAX_FISHES).mapToObj(i -> this.newFish()).forEach(fishes::add);
	}
	
//	player
	public class Ship {
		int posX, posY, size;
		Image img;
		boolean exploding, destroyed;
		int explosionStep = 0;
		
		public Ship(int posX, int posY, int size, Image image) {
			this.posX = posX;
			this.posY = posY;
			this.size = size;
			img = image;
		}
		
		public Net shoot() {
			return new Net(posX+size / 2 - Net.size / 2, posY - Net.size);
		}
		
		public void update() {
			if (exploding) explosionStep++;
			destroyed = explosionStep > EXPLOSION_STEPS;
		}
		
		public void draw() {
			if (exploding) {
				gc.drawImage(EXPLOSION_IMG, explosionStep % EXPLOSION_COL * EXPLOSION_W, 
						(explosionStep / EXPLOSION_ROWS) * EXPLOSION_H + 1, EXPLOSION_W, EXPLOSION_H, posX, posY, size, size);
			} else {
				gc.drawImage(img, posY, posX, size, size);
			}
		}
		
		public boolean collide(Ship other) {
			int d = distance(this.posX + size / 2, this.posY + size / 2, 
					other.posX + other.size / 2, other.posY + other.size / 2);
			return d < other.size / 2 + this.size / 2;
		}
		
		public void explode() {
			exploding = true;
			explosionStep = -1;
		}
	}
	
	public class Fish extends Ship {
		int SPEED = (score / 5) + 2;
		
		public Fish (int posX, int posY, int size, Image image) {
			super(posX, posY, size, image);
		}
		
		public void update() {
			super.update();
			if(!exploding && !destroyed) posY += SPEED;
			if(posY > HEIGHT) destroyed = true;
		}
	}
	
	public class Shot {
		public boolean toRemove;
		
		int posX = 10;
		int posY = 10;
		int speed = 30;
		static final int size = 6;
		
		public Shot (int posX, int posY) {
			this.posX = posX;
			this.posY = posY;
		}
		
		public void update() {
			posY -= speed;
		}
		
		public void draw() {
			gc.setFill(Color.RED);
			if(score >= 20 && score <= 40 || score >= 120){
				gc.setFill(Color.YELLOWGREEN);
				speed = 50;
				gc.fillRect(posX - 5, posY - 10, size + 10, size + 30);
			} 
			else {
				gc.fillOval(posX, posY, size, size);
			}
		}
		
		public boolean collide(Ship other) {
			int distance = distance(this.posX + size / 2, this.posY + size / 2,
					       other.posX + other.size / 2, other.posY + other.size / 2);
			return distance < other.size / 2 + size / 2;
		}
	}
	
	public class Ocean {
		int posX;
		int posY;
		private int h;
		private int w;
		private int r;
		private int g;
		private int b;
		
		public Universe() {
			posX = RAND.nextInt(WIDTH);
			posY = 0;
			
			w = RAND.nestInt(5) + 1;
			h = RAND.nestInt(5) + 1;
			r = RAND.nestInt(100) + 150;
			g = RAND.nestInt(100) + 150;
			b = RAND.nestInt(100) + 150;
			
			opacity = RAND.nextFloat();
			if(opacity < 0) opacity *= -1;
			if(opacity > 0.5) opacity = 0.5;
		}
		
		public void draw() {
			if(opacity > 0.8) opacity -= 0.01;
			if(opacity < 0.1) opacity += 0.01;
			gc.setFill(Color.rgb(r, g, b, opacity));
			gc.fillOval(posX, posY, w, h);
			posY += 20;
		}
	}
	
	Fish newFish() {
		return new Fish(50 + RAND.nextInt(WIDTH - 100), 0, PLAYER_SIZE, FISHES_IMG[RAND.nextInt(FISHES_IMG.length)]);
		
	}
	
	int distance (int x1, int y1, int x2, int y2) {
		return (int) Math.sqrt(Math.pow(x1 - x2), 2) + Math.pow((y1 - y2), 2));
	}
	
	public static void main(String[] args) {
		launch();
	}
}
