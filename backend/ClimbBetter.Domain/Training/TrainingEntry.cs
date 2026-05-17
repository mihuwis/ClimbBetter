namespace ClimbBetter.Domain.Training;

public class TrainingEntry
{
public Guid Id { get; set; }

public Guid TrainingSessionId { get; set; }
public TrainingSession TrainingSession { get; set; } = null!;

public Guid ClimbId { get; set; }
public Climb Climb { get; set; } = null!;

public int DifficultyId { get; set; }
public Difficulty Difficulty { get; set; } = null!;

public int QualityId { get; set; }
public Quality Quality { get; set; } = null!;

public AscentCategory AscentCategory { get; set; }

public bool IsSuccessful { get; set; }

public int? AttemptNumberOnClimb { get; set; }

public int ExecutedMoves { get; set; }
public int? TotalMoves { get; set; }

public decimal LengthMultiplierSnapshot { get; set; }
public int DifficultyPointsSnapshot { get; set; }
public decimal QualityMultiplierSnapshot { get; set; }
public decimal PointsSnapshot { get; set; }

public string? Notes { get; set; }

public Guid? ClientEntryId { get; set; }
public DateTime CreatedAt { get; set; }
}