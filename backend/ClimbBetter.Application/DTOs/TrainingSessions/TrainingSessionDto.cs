namespace ClimbBetter.Application.DTOs.TrainingSessions;


public class TrainingSessionDto
{
    public Guid Id {get; set; }

    public DateTime Date { get; set; }

    public TimeSpan Duration { get; set; }
}