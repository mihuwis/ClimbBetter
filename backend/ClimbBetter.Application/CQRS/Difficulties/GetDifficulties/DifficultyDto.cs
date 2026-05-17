namespace ClimbBetter.Application.CQRS.Difficulties.GetDifficulties;

public class DifficultyDto
{
    public int Id { get; set; }
    public string Name { get; set; } = default!;
    public int Points { get; set; }
}