#pragma once

// ═══════════════════════════════════════════════════════════
// SUBWAY BRUTAL — OFFSETS FILE
// Target: com.kiloo.subwaysurf v3.67.0
// Engine: Unity IL2CPP ARM32
// UPDATE GUIDE: See offset_update_guide.md
// ═══════════════════════════════════════════════════════════

// PLAYER CONTROLLER FIELD OFFSETS
#define OFFSET_RB                   0x10
#define OFFSET_MOVE_SPEED           0x18
#define OFFSET_JUMP_FORCE           0x1C
#define OFFSET_IS_GROUNDED          0x20
#define OFFSET_VELOCITY             0x24
#define OFFSET_GRAVITY_SCALE        0x28

// RUN MANAGER FIELD OFFSETS
#define OFFSET_BASE_RUN_SPEED       0x10
#define OFFSET_MAX_RUN_SPEED        0x14
#define OFFSET_ACCEL_RATE           0x18
#define OFFSET_IS_RUNNING           0x1C
#define OFFSET_IS_SPRINTING         0x1D
#define OFFSET_CURRENT_SPEED        0x20

// JUMP CONTROLLER FIELD OFFSETS
#define OFFSET_MAX_JUMP_HEIGHT      0x10
#define OFFSET_JUMP_TIME            0x14
#define OFFSET_IS_JUMPING           0x20
#define OFFSET_JUMP_COUNT           0x24
#define OFFSET_MAX_JUMPS            0x28

// SCORE MANAGER FIELD OFFSETS
#define OFFSET_CURRENT_SCORE        0x10
#define OFFSET_TOTAL_COINS          0x14
#define OFFSET_HIGH_SCORE           0x18

// HEALTH MANAGER FIELD OFFSETS
#define OFFSET_MAX_HEALTH           0x10
#define OFFSET_CURRENT_HEALTH       0x14
#define OFFSET_IS_DEAD              0x18
#define OFFSET_IS_INVULNERABLE      0x19
#define OFFSET_INVULN_DURATION      0x1C

// FLIGHT CONTROLLER FIELD OFFSETS
#define OFFSET_FLIGHT_SPEED         0x10
#define OFFSET_LIFT_FORCE           0x14
#define OFFSET_MAX_FUEL             0x18
#define OFFSET_CURRENT_FUEL         0x1C
#define OFFSET_IS_FLYING            0x20

// GAME MANAGER FIELD OFFSETS
#define OFFSET_IS_PAUSED            0x10
#define OFFSET_IS_GAME_OVER         0x11
#define OFFSET_CURRENT_LEVEL        0x14

// METHOD RVAs
#define RVA_PLAYER_UPDATE           0x1520
#define RVA_PLAYER_FIXED_UPDATE     0x1700
#define RVA_PLAYER_MOVE             0x1940
#define RVA_COLLISION_ENTER         0x1C00
#define RVA_START_RUNNING           0x3000
#define RVA_STOP_RUNNING            0x3200
#define RVA_UPDATE_SPEED            0x3450
#define RVA_JUMP                    0x2100
#define RVA_CHECK_GROUND            0x2350
#define RVA_ADD_SCORE               0x6100
#define RVA_ADD_COINS               0x6340
#define RVA_TAKE_DAMAGE             0x1000
#define RVA_HEAL                    0x1180
#define RVA_DIE                     0x1320
#define RVA_RESPAWN                 0x1550
#define RVA_START_FLIGHT            0x1000
#define RVA_END_FLIGHT              0x1250
#define RVA_APPLY_FLIGHT_PHYSICS    0x1500
#define RVA_CONSUME_FUEL            0x1800
#define RVA_GAME_OVER               0x1450

// HACK VALUES
#define SPEED_MULTIPLIER            3.0f
#define SUPER_JUMP_HEIGHT           25.0f
#define MAX_JUMPS_INFINITE          999
#define COINS_ADD_AMOUNT            99999
#define SCORE_ADD_AMOUNT            999999
#define FUEL_MAX_VALUE              9999.0f
