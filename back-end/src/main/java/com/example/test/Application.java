package com.example.test;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
@RestController
@SpringBootApplication
public class Application {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	private static final String secret = "mysecret";

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@PostMapping("/register")
	public ResponseEntity<String> register(@RequestBody Users user) {
		String query = "SELECT * FROM users WHERE username = ?";
		List<Users> existingUsers = jdbcTemplate.query(query, new Object[]{user.getUsername()}, new BeanPropertyRowMapper<>(Users.class));
		if (!existingUsers.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email is already registered");
		}

		String hashedPassword = passwordEncoder.encode(user.getPassword());
		String insertQuery = "INSERT INTO users (username, email, password) VALUES (?, ?, ?)";
		jdbcTemplate.update(insertQuery, user.getUsername(), user.getEmail(), hashedPassword);
		return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
	}

	@GetMapping("test")
	public ResponseEntity<String> testEndpoint() {
    return ResponseEntity.ok().body("This is a test endpoint");
}
	
	@PostMapping("/login")
	public ResponseEntity<String> login(@RequestBody Users user) {
		System.err.println("5555");
		String query = "SELECT * FROM users WHERE username = ?";
		Users existingUser = jdbcTemplate.queryForObject(query, new Object[]{user.getUsername()}, new BeanPropertyRowMapper<>(Users.class));
		if (existingUser == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid email or password");
		}

		if (!passwordEncoder.matches(user.getPassword(), existingUser.getPassword())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid email or password");
		}

		String token = Jwts.builder()
				.setSubject(user.getUsername())
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hour
				.signWith(SignatureAlgorithm.HS512, secret)
				.compact();

		return ResponseEntity.ok().body("Login successful\nToken: " + token);
	}

	@PostMapping("/create_post")
	public ResponseEntity<String> createPost(@RequestBody Post post, HttpServletRequest request) {
		String username = getUsernameFromToken(request);
		if (username == null) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid or expired token");
		}

		String getUserIdQuery = "SELECT user_id FROM users WHERE username = ?";
		Integer userId = jdbcTemplate.queryForObject(getUserIdQuery, Integer.class, username);
		if (userId == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
		}

		String insertPostQuery = "INSERT INTO posts (user_id, title, description, max_participants, category, event_start) VALUES (?, ?, ?, ?, ?, ?)";
		jdbcTemplate.update(insertPostQuery, userId, post.getTitle(), post.getDescription(), post.getMaxParticipants(), post.getCategory(), post.getEventStart());

		return ResponseEntity.status(HttpStatus.CREATED).body("Post created successfully");
	}

	@PostMapping("/participants")
	public ResponseEntity<String> addParticipant(@RequestBody Participant participant, HttpServletRequest request) {
		String username = getUsernameFromToken(request);
		if (username == null) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid or expired token");
		}

		String getUserIdQuery = "SELECT user_id FROM users WHERE username = ?";
		Integer userId = jdbcTemplate.queryForObject(getUserIdQuery, Integer.class, username);
		if (userId == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
		}

		String insertParticipantQuery = "INSERT INTO participants (post_id, user_id, joined_at) VALUES (?, ?, NOW())";
		jdbcTemplate.update(insertParticipantQuery, participant.getPostId(), userId);

		return ResponseEntity.ok().body("Participant added successfully");
	}

	@GetMapping("/posts")
	public ResponseEntity<List<Post>> getPosts() {
		String query = "SELECT * FROM posts";
		List<Post> posts = jdbcTemplate.query(query, new BeanPropertyRowMapper<>(Post.class));
		if (posts.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
		} else {
			return ResponseEntity.ok().body(posts);
		}
	}

	@GetMapping("/user_activities")
	public ResponseEntity<List<Participant>> getUserActivities(HttpServletRequest request) {
		String username = getUsernameFromToken(request);
		if (username == null) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
		}

		String getUserIdQuery = "SELECT user_id FROM users WHERE username = ?";
		Integer userId = jdbcTemplate.queryForObject(getUserIdQuery, Integer.class, username);
		if (userId == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
		}

		String query = "SELECT * FROM participants WHERE user_id = ?";
		List<Participant> activities = jdbcTemplate.query(query, new BeanPropertyRowMapper<>(Participant.class), userId);
		return ResponseEntity.ok().body(activities);
	}

	@GetMapping("/user_posts")
	public ResponseEntity<List<Post>> getUserPosts(HttpServletRequest request) {
		String username = getUsernameFromToken(request);
		if (username == null) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
		}

		String getUserIdQuery = "SELECT user_id FROM users WHERE username = ?";
		Integer userId = jdbcTemplate.queryForObject(getUserIdQuery, Integer.class, username);
		if (userId == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
		}

		String query = "SELECT * FROM posts WHERE user_id = ?";
		List<Post> posts = jdbcTemplate.query(query, new BeanPropertyRowMapper<>(Post.class), userId);
		return ResponseEntity.ok().body(posts);
	}

	private String getUsernameFromToken(HttpServletRequest request) {
		String token = request.getHeader("Authorization");
		if (token == null || !token.startsWith("Bearer ")) {
			return null;
		}
		token = token.substring(7);
		return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody().getSubject();
	}
}
