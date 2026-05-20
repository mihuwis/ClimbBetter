namespace ClimbBetter.Application.CQRS.TrainingSessions.GetTrainingSessions;

public class TrainingSessionListItemDto
{
    public Guid Id { get; set; }
    public DateTime Date { get; set; }
    public TimeSpan? Duration { get; set; }
    public string? Goal { get; set; }
    public string? Method { get; set; }
    public Guid? LocationId { get; set; }
    public string? LocationName { get; set; }
    public int EntriesCount { get; set; }
    public int TotalMoves { get; set; }
    public decimal TotalPoints { get; set; }
}