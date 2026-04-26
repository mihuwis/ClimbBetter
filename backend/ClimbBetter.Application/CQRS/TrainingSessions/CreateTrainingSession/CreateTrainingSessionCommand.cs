using ClimbBetter.Application.DTOs;
using ClimbBetter.Application.DTOs.TrainingSessions;
using MediatR;

namespace ClimbBetter.Application.CQRS.TrainingSessions.CreateTrainingSession;


public class CreateTrainingSessionCommand : IRequest<Guid>
{
    // this guid is temp soution, before implementation

    public DateTime Date { get; set; }
    public TimeSpan Duration { get; set; }
}