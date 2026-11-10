extends CharacterBody3D
class_name Enemy

export var patrol_speed := 2.0
export var chase_speed := 4.0
export var detection_range := 8.0
export var attack_range := 2.0
export var attack_cooldown := 2.0
export var health := 50.0

var state := "idle"  # idle, patrol, detect, chase, attack, die
var patrol_points: Array = []
var patrol_index := 0
var _target := null
var _attack_timer := 0.0
var _die_anim := false

@onready var _anim_player := $AnimationPlayer

func _ready():
	if has_node("AnimationPlayer"):
		_anim_player.play("idle")

func _physics_process(delta: float):
	if state == "die":
		return
	
	_attack_timer -= delta if _attack_timer > 0 else 0
	
	var player = get_tree().get_first_node_in_group("player")
	if not player:
		return
	
	var dist = global_transform.origin.distance_to(player.global_transform.origin)
	
	if dist <= attack_range and _attack_timer <= 0:
		state = "attack"
		_attack_timer = attack_cooldown
		player.take_damage(10.0)
		_anim_player.play("attack") if _anim_player else null
	elif dist <= detection_range:
		state = "chase"
		velocity = global_transform.origin.direction_to(player.global_transform.origin) * chase_speed
		move_and_slide()
	elif state == "patrol" and patrol_points.size() > 0:
		var target = patrol_points[patrol_index]
		var dist_to_target = global_transform.origin.distance_to(target)
		if dist_to_target < 1.0:
			patrol_index = (patrol_index + 1) % patrol_points.size()
		else:
			velocity = global_transform.origin.direction_to(target) * patrol_speed
			move_and_slide()
	else:
		velocity = Vector3.ZERO

func take_damage(amount: float):
	health -= amount
	if health <= 0:
		state = "die"
		_anim_player.play("die") if _anim_player else null
		queue_free()
