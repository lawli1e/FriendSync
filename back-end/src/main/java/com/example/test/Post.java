package com.example.test;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "posts")
public class Post {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private String description;

	@Column(nullable = false)
	private int maxParticipants;

	@Column(nullable = false)
	private String category;

	@Column(nullable = false)
	private Date eventStart;

	public Object getTitle() {
        return null;
    }

	public Object getDescription() {
        return null;
    }

	public Object getMaxParticipants() {
        return null;
    }

	public Object getCategory() {
        return null;
    }

	public Object getEventStart() {
        return null;
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setMaxParticipants(int maxParticipants) {
		this.maxParticipants = maxParticipants;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public void setEventStart(Date eventStart) {
		this.eventStart = eventStart;
	}

	// constructors, getters, setters, and other methods
}
